package io.github.koka99cab.multisolo.core.message;

/** Votes adjust a message's total lifetime from publication, never from the last read. */
public final class MessageLifetime {
    public static final long DAY_MILLIS = 24L * 60 * 60 * 1000;
    public static final int BASE_DAYS = 7;
    public static final int MIN_DAYS = 1;
    public static final int MAX_DAYS = 30;
    private MessageLifetime() {}
    public static long durationMillis(int upvotes, int downvotes) {
        if (upvotes < 0 || downvotes < 0 || upvotes + (long) downvotes > 1000) throw new IllegalArgumentException("Invalid votes");
        return Math.clamp(BASE_DAYS + upvotes - downvotes, MIN_DAYS, MAX_DAYS) * DAY_MILLIS;
    }
    public static long expiresAt(long createdAt, int upvotes, int downvotes) {
        long duration = durationMillis(upvotes, downvotes);
        return createdAt > Long.MAX_VALUE - duration ? Long.MAX_VALUE : createdAt + duration;
    }
    public static boolean activeAt(long createdAt, int upvotes, int downvotes, long now) {
        return now >= createdAt && now - createdAt < durationMillis(upvotes, downvotes);
    }
}
