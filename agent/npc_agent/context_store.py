from __future__ import annotations

import json
import re
import shutil
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from pydantic_ai.messages import ModelMessage, ModelMessagesTypeAdapter

from .config import AgentConfig
from .schemas import (
    ConversationEndRequest,
    DeliberateAgentRequest,
    DeliberateAgentResponse,
    FastAgentRequest,
    FastAgentResponse,
)


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

    def save_messages(self, messages_json: bytes) -> None:
        self.messages_path.write_bytes(messages_json)
        self.compact_if_needed()

    def compact_if_needed(self) -> bool:
        if self.max_history_bytes <= 0 or not self.messages_path.exists():
            return False
        if self.messages_path.stat().st_size <= self.max_history_bytes:
            return False
        summary = summarize_json_bytes(self.messages_path.read_bytes(), "Context compressed because message history exceeded limit.")
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

    def end_conversation(self, request: ConversationEndRequest) -> bool:
        message_bytes = self.messages_path.read_bytes() if self.messages_path.exists() else b"[]"
        summary_text = self.summary_path.read_text(encoding="utf-8") if self.summary_path.exists() else ""
        if message_bytes.strip() in (b"", b"[]") and not meaningful_markdown(summary_text):
            self.append_history({"type": "conversation_ended", "reason": request.reason, "memory_updated": False})
            return False
        memory = summarize_json_bytes(
            message_bytes,
            f"Conversation ended because {request.reason}. Preserve stable facts, player preferences, promises, and unresolved goals.",
        )
        if meaningful_markdown(summary_text):
            memory = summary_text.strip() + "\n\n" + memory
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
        safe_id = sanitize(agent_id)
        safe_kind = "master" if kind == "master" else "npc"
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


def summarize_json_bytes(data: bytes, prefix: str) -> str:
    text = data.decode("utf-8", errors="replace")
    compact = " ".join(text.split())
    if len(compact) > 4000:
        compact = compact[-4000:]
    return f"{prefix}\n\n{compact}"
