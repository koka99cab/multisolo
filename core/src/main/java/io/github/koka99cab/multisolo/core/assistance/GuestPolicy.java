package io.github.koka99cab.multisolo.core.assistance;

import java.util.Objects;

/** Server-side policy contract. Minecraft enforcement is a separate adapter. */
public final class GuestPolicy {
    private GuestPolicy() {}

    public static boolean allows(GuestAction action) {
        return switch (Objects.requireNonNull(action, "action")) {
            case MOVE, LOOK, ATTACK_HOSTILE -> true;
            default -> false;
        };
    }
}
