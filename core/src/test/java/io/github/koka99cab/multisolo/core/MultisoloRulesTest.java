package io.github.koka99cab.multisolo.core;

import io.github.koka99cab.multisolo.core.assistance.GuestAction;
import io.github.koka99cab.multisolo.core.assistance.GuestPolicy;
import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.experience.Difficulty;
import io.github.koka99cab.multisolo.core.experience.Experience;
import io.github.koka99cab.multisolo.core.social.SocialConsent;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultisoloRulesTest {
    private final Experience original = new Experience(
            "multisolo:origin", 1, 20261012L, Difficulty.NORMAL, Map.of());

    private CompatibilityKey key(Experience experience) {
        return new CompatibilityKey(experience, "26.4-snapshot-3", 1, "minecraft:overworld");
    }

    @Test
    void independentWorldsWithSameRulesCanShare() {
        Experience otherSave = new Experience(
                "multisolo:origin", 1, 20261012L, Difficulty.NORMAL, Map.of());
        assertTrue(key(original).canShareWith(key(otherSave)));
        assertFalse(key(original).canShareWith(null));
    }

    @Test
    void changedSeedDifficultyOrRevisionSeparatesPlayers() {
        assertFalse(key(original).canShareWith(key(new Experience(
                original.id(), 1, 1L, Difficulty.NORMAL, Map.of()))));
        assertFalse(key(original).canShareWith(key(new Experience(
                original.id(), 1, original.seed(), Difficulty.HARD, Map.of()))));
        assertFalse(key(original).canShareWith(key(new Experience(
                original.id(), 2, original.seed(), Difficulty.NORMAL, Map.of()))));
    }

    @Test
    void gameProtocolDimensionAndWorldgenAreIsolated() {
        assertFalse(key(original).canShareWith(new CompatibilityKey(
                original, "26.3", 1, "minecraft:overworld")));
        assertFalse(key(original).canShareWith(new CompatibilityKey(
                original, "26.4-snapshot-3", 2, "minecraft:overworld")));
        assertFalse(key(original).canShareWith(new CompatibilityKey(
                original, "26.4-snapshot-3", 1, "minecraft:the_nether")));
        assertFalse(key(original).canShareWith(key(new Experience(
                original.id(), 1, original.seed(), Difficulty.NORMAL, Map.of("worldgen", "1.0")))));
    }

    @Test
    void callerCannotMutateAnExperienceAfterCreation() {
        Map<String, String> mods = new HashMap<>();
        mods.put("worldgen", "1.0");
        Experience experience = new Experience("multisolo:test", 1, 0, Difficulty.NORMAL, mods);
        mods.put("worldgen", "2.0");
        assertEquals("1.0", experience.worldgenMods().get("worldgen"));
        assertThrows(UnsupportedOperationException.class, () -> experience.worldgenMods().clear());
    }

    @Test
    void invalidRulesAreRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Experience("missing_namespace", 1, 0, Difficulty.NORMAL, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Experience("multisolo:test", 0, 0, Difficulty.NORMAL, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new Experience("multisolo:test", 1, 0, Difficulty.NORMAL, Map.of("mod", "")));
    }

    @Test
    void assistanceRequiresBothPlayersAndDefaultsToOffline() {
        SocialConsent optIn = new SocialConsent(true, true, true);
        assertFalse(SocialConsent.offline().canAssistWith(optIn));
        assertFalse(optIn.canAssistWith(SocialConsent.offline()));
        assertTrue(optIn.canAssistWith(optIn));
    }

    @Test
    void guestCannotChangeLootInventoryWorldOrDifficulty() {
        for (GuestAction action : new GuestAction[]{
                GuestAction.PICK_UP_ITEM, GuestAction.DROP_ITEM, GuestAction.MOVE_INVENTORY,
                GuestAction.OPEN_CONTAINER, GuestAction.CRAFT, GuestAction.GAIN_EXPERIENCE,
                GuestAction.BREAK_BLOCK, GuestAction.PLACE_BLOCK, GuestAction.INTERACT_WORLD,
                GuestAction.DAMAGE_PLAYER, GuestAction.USE_COMMAND}) {
            assertFalse(GuestPolicy.allows(action), action.name());
        }
        assertTrue(GuestPolicy.allows(GuestAction.ATTACK_HOSTILE));
    }
}
