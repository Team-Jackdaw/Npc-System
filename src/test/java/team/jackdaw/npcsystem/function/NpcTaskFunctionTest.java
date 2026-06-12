package team.jackdaw.npcsystem.function;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NpcTaskFunctionTest {
    @Test
    void stringArgReadsPrimaryArgument() {
        assertEquals("Steve", NpcTaskFunction.stringArg(Map.of("player", " Steve "), "player", "player_name"));
    }

    @Test
    void stringArgReadsAliasesWhenPrimaryIsMissing() {
        assertEquals("Alex", NpcTaskFunction.stringArg(Map.of("player_name", "Alex"), "player", "player_name"));
        assertEquals("WDRSerenaSuki", NpcTaskFunction.stringArg(Map.of("target_player", "WDRSerenaSuki"), "player", "player_name", "target_player"));
    }

    @Test
    void stringArgReturnsNullForBlankOrMissingValues() {
        assertNull(NpcTaskFunction.stringArg(Map.of("player_name", " "), "player", "player_name"));
        assertNull(NpcTaskFunction.stringArg(Map.of(), "player", "player_name"));
    }
}
