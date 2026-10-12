package io.github.koka99cab.multisolo.core.echo;

/** Only motion and a generic tool category; never inventory, skin URL, name or block changes. */
public record EchoFrame(double x, double y, double z, float yaw, float pitch,
                        boolean crouching, boolean sprinting, float swing, Tool tool) {
    public enum Tool { HAND, PICKAXE, AXE, SHOVEL, HOE, SWORD }
    public EchoFrame {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)
                || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000 || Math.abs(y) > 2048
                || !Float.isFinite(yaw) || Math.abs(yaw) > 180 || !Float.isFinite(pitch) || Math.abs(pitch) > 90
                || !Float.isFinite(swing) || swing < 0 || swing > 1 || tool == null)
            throw new IllegalArgumentException("Invalid echo frame");
    }
    public double distanceSquared(EchoFrame other) {
        double dx = x - other.x, dy = y - other.y, dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }
    public EchoFrame interpolate(EchoFrame next, double fraction) {
        double t = Math.clamp(fraction, 0, 1);
        float turn = (next.yaw - yaw + 540) % 360 - 180;
        float rotation = (float) ((yaw + turn * t + 540) % 360 - 180);
        return new EchoFrame(x + (next.x - x) * t, y + (next.y - y) * t, z + (next.z - z) * t,
                rotation, (float) (pitch + (next.pitch - pitch) * t), crouching, sprinting,
                (float) (swing + (next.swing - swing) * t), tool);
    }
}
