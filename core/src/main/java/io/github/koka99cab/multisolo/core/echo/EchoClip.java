package io.github.koka99cab.multisolo.core.echo;

import io.github.koka99cab.multisolo.core.experience.CompatibilityKey;
import io.github.koka99cab.multisolo.core.message.MessagePosition;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record EchoClip(UUID id, String authorId, CompatibilityKey scope, List<EchoFrame> frames, long createdAt) {
    public static final int SAMPLE_MILLIS = 200;
    public static final int MAX_FRAMES = 41;
    public static final long LIFETIME_MILLIS = 15 * 60_000L;
    public EchoClip {
        Objects.requireNonNull(id); Objects.requireNonNull(scope);
        if (authorId == null || !authorId.matches("[a-f0-9]{64}") || createdAt < 0) throw new IllegalArgumentException("Invalid echo identity/time");
        frames = validateFrames(frames);
    }
    public static List<EchoFrame> validateFrames(List<EchoFrame> frames) {
        frames = List.copyOf(frames);
        if (frames.size() < 2 || frames.size() > MAX_FRAMES) throw new IllegalArgumentException("Invalid echo duration");
        for (int i = 1; i < frames.size(); i++) {
            if (frames.getFirst().distanceSquared(frames.get(i)) > 64 * 64
                    || frames.get(i - 1).distanceSquared(frames.get(i)) > 8 * 8)
                throw new IllegalArgumentException("Echo teleport or oversized path");
        }
        return frames;
    }
    public MessagePosition anchor() {
        var frame = frames.getFirst();
        return new MessagePosition((int) Math.floor(frame.x()), (int) Math.floor(frame.y()), (int) Math.floor(frame.z()));
    }
    public boolean activeAt(long now) { return now >= createdAt && now - createdAt < LIFETIME_MILLIS; }
    public long durationMillis() { return (frames.size() - 1L) * SAMPLE_MILLIS; }
    public EchoFrame at(double millis) {
        double index = Math.clamp(millis / SAMPLE_MILLIS, 0, frames.size() - 1);
        int left = (int) index;
        return left == frames.size() - 1 ? frames.getLast() : frames.get(left).interpolate(frames.get(left + 1), index - left);
    }
}
