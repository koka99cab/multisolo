package io.github.koka99cab.multisolo.core.message;

import java.util.List;

/** Four sign lines made exclusively of catalog IDs, never player-supplied strings. */
public record MessagePhrase(List<Line> lines) {
    public MessagePhrase {
        lines = List.copyOf(lines);
        if (lines.size() != 4) throw new IllegalArgumentException("A sign has exactly four lines");
        if (lines.stream().allMatch(line -> line == Line.EMPTY)) throw new IllegalArgumentException("An empty sign cannot be shared");
    }
    public enum Line {
        EMPTY, BEWARE, AHEAD, SEEK, TRY, BUT, THEREFORE,
        ENEMY, AMBUSH, LAVA, WATER, FALL, CAVE, PASSAGE, VILLAGE,
        CHEST, TREASURE, ORE, SHELTER, JUMP, PATIENCE
    }
    public static MessagePhrase example() {
        return new MessagePhrase(List.of(Line.BEWARE, Line.LAVA, Line.BUT, Line.TREASURE));
    }
}
