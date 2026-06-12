from __future__ import annotations

import json
import re
import shutil
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Awaitable, Callable

from pydantic_ai.messages import ModelMessage, ModelMessagesTypeAdapter

from .config import AgentConfig
from .schemas import (
    ConversationEndRequest,
    DeliberateAgentRequest,
    FastAgentRequest,
)

SummaryFunction = Callable[[str, dict[str, Any]], Awaitable[str]]


DEFAULT_AGENTS = """# Agent Profile

You are a Minecraft NPC controlled through structured JSON actions.
Use available tools only when they are present in the current request.
Keep replies concise and act consistently with the latest Minecraft snapshot.
"""

DEFAULT_MASTER_AGENTS = """# Master Profile

You are the high-permission Master agent for a Minecraft server.
Only call administrator-level commands when the administrator intent is explicit.
"""

DEFAULT_SOLU = """# Current Solution

No active long-term plan yet.
"""

DEFAULT_SUMMARY = """# Summary

"""

DEFAULT_MEMORY = """# Memory

"""

TEMPLATE_ROOT = Path(__file__).resolve().parents[1] / "templates"


@dataclass
class AgentContext:
    root: Path
    agent_id: str
    kind: str
    max_history_bytes: int

    @property
    def agents_path(self) -> Path:
        return self.root / "AGENTS.md"

    @property
    def solu_path(self) -> Path:
        return self.root / "SOLU.md"

    @property
    def summary_path(self) -> Path:
        return self.root / "SUMMARY.md"

    @property
    def memory_path(self) -> Path:
        return self.root / "MEMORY.md"

    @property
    def messages_path(self) -> Path:
        return self.root / "messages.json"

    @property
    def history_path(self) -> Path:
        return self.root / "history.jsonl"

    def initialize(self) -> None:
        self.root.mkdir(parents=True, exist_ok=True)
        self._write_default(
            self.agents_path,
            DEFAULT_MASTER_AGENTS if self.kind == "master" else DEFAULT_AGENTS,
            TEMPLATE_ROOT / self.kind / "AGENTS.md",
        )
        self._write_default(self.solu_path, DEFAULT_SOLU, TEMPLATE_ROOT / "common" / "SOLU.md")
        self._write_default(self.summary_path, DEFAULT_SUMMARY, TEMPLATE_ROOT / "common" / "SUMMARY.md")
        self._write_default(self.memory_path, DEFAULT_MEMORY, TEMPLATE_ROOT / "common" / "MEMORY.md")
        self._write_default(self.messages_path, "[]")
        self._write_default(self.history_path, "")

    def record_request(self, mode: str, payload: dict[str, Any]) -> None:
        self.append_history({"type": "request", "mode": mode, "payload": payload})

    def record_response(self, mode: str, payload: dict[str, Any]) -> None:
        self.append_history({"type": "response", "mode": mode, "payload": payload})

    def append_history(self, event: dict[str, Any]) -> None:
        event = {"time": now_iso(), **event}
        with self.history_path.open("a", encoding="utf-8") as file:
            file.write(json.dumps(event, ensure_ascii=False, default=str) + "\n")

    def message_history(self) -> list[ModelMessage] | None:
        if not self.messages_path.exists() or self.messages_path.stat().st_size == 0:
            return None
        data = self.messages_path.read_bytes()
        if data.strip() in (b"", b"[]"):
            return None
        try:
            return ModelMessagesTypeAdapter.validate_json(data)
        except Exception as exc:
            self.append_history({"type": "message_history_invalid", "error": type(exc).__name__})
            return None

    async def save_messages(self, messages_json: bytes, summarizer: SummaryFunction | None = None) -> bool:
        self.messages_path.write_bytes(messages_json)
        return await self.compact_if_needed(summarizer)

    async def compact_if_needed(self, summarizer: SummaryFunction | None = None) -> bool:
        if self.max_history_bytes <= 0 or not self.messages_path.exists():
            return False
        if self.messages_path.stat().st_size <= self.max_history_bytes:
            return False
        summarizer = summarizer or deterministic_summary
        message_text = self.messages_path.read_text(encoding="utf-8", errors="replace")
        existing_summary = self.summary_path.read_text(encoding="utf-8") if self.summary_path.exists() else ""
        prompt = (
            "请将下面 Minecraft NPC 当前会话上下文压缩成自然语言摘要。"
            "保留正在进行的话题、任务、承诺、玩家偏好、未解决目标和重要世界状态。"
            "不要输出 JSON、字段名列表或调试日志。"
        )
        payload = {
            "agent_id": self.agent_id,
            "kind": self.kind,
            "existing_summary": existing_summary,
            "messages": message_text,
        }
        try:
            summary = await summarizer(prompt, payload)
        except Exception as exc:
            self.append_history({"type": "context_compaction_failed", "error": type(exc).__name__})
            return False
        if not summary or not summary.strip():
            self.append_history({"type": "context_compaction_failed", "error": "empty_summary"})
            return False
        append_markdown_section(self.summary_path, "Compressed Context", summary)
        self.messages_path.write_text("[]", encoding="utf-8")
        self.append_history({"type": "context_compacted", "summary": summary})
        return True

    def apply_memory_updates(self, updates: list[str] | None) -> None:
        if not updates:
            return
        for update in updates:
            if update and update.strip():
                append_markdown_section(self.memory_path, "Memory Update", update.strip())
        self.append_history({"type": "memory_updates", "count": len(updates)})

    async def end_conversation(self, request: ConversationEndRequest, summarizer: SummaryFunction | None = None) -> bool:
        message_text = self.messages_path.read_text(encoding="utf-8", errors="replace") if self.messages_path.exists() else "[]"
        summary_text = self.summary_path.read_text(encoding="utf-8") if self.summary_path.exists() else ""
        if message_text.strip() in ("", "[]") and not meaningful_markdown(summary_text):
            self.append_history({"type": "conversation_ended", "reason": request.reason, "memory_updated": False})
            return False
        summarizer = summarizer or deterministic_summary
        prompt = (
            "请根据当前 Minecraft NPC 会话记录和 SUMMARY.md 上下文，生成要写入 MEMORY.md 的长期自然语言记忆。"
            "只保留稳定事实、玩家偏好、NPC 承诺、重要事件、未完成目标和后续需要记住的关系。"
            "不要输出 JSON、字段名列表或原始消息转储。"
        )
        payload = {
            "agent_id": self.agent_id,
            "kind": self.kind,
            "reason": request.reason,
            "snapshot": request.snapshot,
            "summary": summary_text,
            "messages": message_text,
        }
        try:
            memory = await summarizer(prompt, payload)
        except Exception as exc:
            self.append_history({"type": "conversation_end_summary_failed", "reason": request.reason, "error": type(exc).__name__})
            return False
        if not memory or not memory.strip():
            self.append_history({"type": "conversation_end_summary_failed", "reason": request.reason, "error": "empty_memory"})
            return False
        append_markdown_section(self.memory_path, "Conversation Memory", memory)
        self.messages_path.write_text("[]", encoding="utf-8")
        self.summary_path.write_text("# Summary\n\n", encoding="utf-8")
        self.append_history({"type": "conversation_ended", "reason": request.reason, "memory_updated": True})
        return True

    def render_context_instructions(self) -> str:
        return "\n\n".join(
            [
                self.agents_path.read_text(encoding="utf-8").strip(),
                self.solu_path.read_text(encoding="utf-8").strip(),
                self.summary_path.read_text(encoding="utf-8").strip(),
                self.memory_path.read_text(encoding="utf-8").strip(),
            ]
        )

    @staticmethod
    def _write_default(path: Path, content: str, template_path: Path | None = None) -> None:
        if not path.exists():
            if template_path is not None and template_path.exists():
                path.write_text(template_path.read_text(encoding="utf-8"), encoding="utf-8")
            else:
                path.write_text(content, encoding="utf-8")


class AgentContextStore:
    def __init__(self, config: AgentConfig):
        self.base = Path(config.state_dir)
        self.max_history_bytes = config.max_history_bytes
        self._cleanup_legacy_storage_dirs()

    def for_fast(self, request: FastAgentRequest) -> AgentContext:
        agent_id = request.npc.id or request.rid
        return self._context(agent_id, request.npc.kind)

    def for_deliberate(self, request: DeliberateAgentRequest) -> AgentContext:
        agent_id = request.npc.uuid or request.request_id
        return self._context(agent_id, request.npc.kind)

    def for_end(self, request: ConversationEndRequest) -> AgentContext:
        agent_id = request.npc.uuid or request.npc.id or request.request_id
        return self._context(agent_id, request.npc.kind)

    def _context(self, agent_id: str, kind: str) -> AgentContext:
        safe_kind = "master" if kind == "master" else "npc"
        safe_id = "default" if safe_kind == "master" else sanitize(agent_id)
        context = AgentContext(
            root=self.base / safe_kind / safe_id,
            agent_id=safe_id,
            kind=safe_kind,
            max_history_bytes=self.max_history_bytes,
        )
        context.initialize()
        return context

    def _cleanup_legacy_storage_dirs(self) -> None:
        candidates = [self.base / "memory", self.base / "rag"]
        if self.base.name == "agent-state":
            candidates.extend([self.base.parent / "memory", self.base.parent / "rag"])
        for path in candidates:
            if path.exists() and path.is_dir():
                shutil.rmtree(path)


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def sanitize(value: str) -> str:
    safe = re.sub(r"[^A-Za-z0-9._-]", "_", value or "unknown")
    return safe[:128] or "unknown"


def append_markdown_section(path: Path, title: str, content: str) -> None:
    timestamp = now_iso()
    with path.open("a", encoding="utf-8") as file:
        file.write(f"\n## {title} - {timestamp}\n\n{content.strip()}\n")


def meaningful_markdown(text: str) -> bool:
    stripped = text.replace("# Summary", "").strip()
    return bool(stripped)


async def deterministic_summary(prompt: str, payload: dict[str, Any]) -> str:
    if "MEMORY.md" in prompt:
        reason = payload.get("reason", "ended")
        return f"本次会话因 {reason} 结束。当前运行在未启用模型总结的模式，未提取新的长期事实。"
    return "当前运行在未启用模型总结的模式。会话仍在进行中，但未提取新的压缩上下文。"
