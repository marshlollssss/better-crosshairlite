package dev.crosshairlite;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.MobEntity;

public final class TargetColors {
    private TargetColors() {}
    public enum Kind { NONE, HOSTILE, PLAYER, PASSIVE }
    public static Kind classify(Entity entity) {
        if(entity instanceof PlayerEntity)return Kind.PLAYER;
        if(entity instanceof Monster)return Kind.HOSTILE;
        if(entity instanceof MobEntity)return Kind.PASSIVE;
        return Kind.NONE;
    }
    public static Integer override(CrosshairConfig c,Kind kind) {
        return switch(kind) {
            case HOSTILE -> c.hostileColorEnabled?c.hostileColor:null;
            case PLAYER -> c.playerColorEnabled?c.playerColor:null;
            case PASSIVE -> c.passiveColorEnabled?c.passiveColor:null;
            case NONE -> null;
        };
    }
}
