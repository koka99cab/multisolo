package io.github.koka99cab.multisolo.core.message;

/** Block coordinates in the dimension carried by the compatibility key. */
public record MessagePosition(int x, int y, int z) {
    public MessagePosition {
        if (Math.abs((long) x) > 30_000_000 || Math.abs((long) z) > 30_000_000
                || y < -2048 || y > 2048) {
            throw new IllegalArgumentException("Position outside supported world bounds");
        }
    }

    public long distanceSquared(MessagePosition other) {
        long dx = (long) x - other.x, dy = (long) y - other.y, dz = (long) z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
}
