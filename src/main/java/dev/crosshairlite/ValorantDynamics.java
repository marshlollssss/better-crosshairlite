package dev.crosshairlite;

import net.minecraft.client.network.ClientPlayerEntity;

/**
 * Drives Valorant-style movement and firing error from what the Minecraft player is doing.
 * Movement error follows speed (and is full while airborne); firing error spikes when you swing,
 * attack, or release a charged item, then settles back smoothly.
 */
public final class ValorantDynamics {
    private ValorantDynamics() {}

    /** Pixels a full error pushes the lines outward, before each line group's multiplier. */
    public static final double MOVE_PX = 6, FIRE_PX = 8;
    private static final double SPRINT_SPEED = 0.28;      // blocks per tick
    private static final double MOVE_RISE = 14, MOVE_FALL = 9; // 1/seconds, exponential smoothing
    private static final double FIRE_DECAY_SECONDS = 0.35;

    private static double movement, firing, prevSwing;
    private static boolean prevUsing;
    private static long lastNanos;

    public static double movement() { return movement; }
    public static double firing() { return firing; }

    public static void reset() { movement = 0; firing = 0; prevSwing = 0; prevUsing = false; lastNanos = 0; }

    /** Call once per rendered frame. */
    public static void update(ClientPlayerEntity player) {
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0.016 : Math.min(0.1, (now - lastNanos) / 1.0e9);
        lastNanos = now;
        if (player == null) { reset(); return; }

        var v = player.getVelocity();
        double speed = Math.sqrt(v.x * v.x + v.z * v.z);
        double target = Math.min(1, speed / SPRINT_SPEED);
        if (!player.isOnGround()) target = 1;
        else if (player.isSneaking()) target *= 0.5;
        double k = target > movement ? MOVE_RISE : MOVE_FALL;
        movement += (target - movement) * (1 - Math.exp(-k * dt));

        double swing = player.getHandSwingProgress(0f);
        boolean using = player.isUsingItem();
        if ((prevSwing == 0 && swing > 0) || (prevUsing && !using)) firing = 1;
        prevSwing = swing;
        prevUsing = using;
        firing = Math.max(0, firing - dt / FIRE_DECAY_SECONDS);
    }
}
