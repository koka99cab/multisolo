package io.github.koka99cab.multisolo.core.social;

/** Social preferences only. They never grant world or inventory permissions. */
public record SocialConsent(boolean messages, boolean echoes, boolean assistance) {
    public static SocialConsent offline() {
        return new SocialConsent(false, false, false);
    }

    public boolean canAssistWith(SocialConsent other) {
        return other != null && assistance && other.assistance;
    }
}
