package dev.crosshairlite;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.Identifier;

/** Remote cooldowns are not synchronized: estimate vanilla duration from the disable sound. */
public final class ShieldTargets {
    private static final ShieldCooldowns cooldowns=new ShieldCooldowns();
    private static Object world;
    private static void updateWorld() {
        Object current=MinecraftClient.getInstance().world;
        if(world!=current){world=current;cooldowns.clear();}
    }
    public static void onSound(PlaySoundS2CPacket packet) {
        updateWorld();var client=MinecraftClient.getInstance();
        if(client.world==null || !packet.getSound().value().id().equals(Identifier.ofVanilla("item.shield.break")))return;
        long now=client.world.getTime();
        var player=client.world.getPlayers().stream().filter(ShieldTargets::holdsShield)
            .filter(p->p.squaredDistanceTo(packet.getX(),packet.getY(),packet.getZ())<2.25)
            .min(java.util.Comparator.comparingDouble(p->p.squaredDistanceTo(packet.getX(),packet.getY(),packet.getZ()))).orElse(null);
        if(player!=null)cooldowns.disable(player.getUuid(),now);
    }
    public static void onEntitySound(net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket packet) {
        updateWorld();var client=MinecraftClient.getInstance();
        if(client.world==null||!packet.getSound().value().id().equals(Identifier.ofVanilla("item.shield.break")))return;
        Entity target=client.world.getEntityById(packet.getEntityId());
        if(target instanceof LivingEntity living&&holdsShield(living))cooldowns.disable(target.getUuid(),client.world.getTime());
    }
    private static boolean holdsShield(LivingEntity entity) {
        return entity.getMainHandStack().isOf(Items.SHIELD)||entity.getOffHandStack().isOf(Items.SHIELD);
    }
    public static Integer color(CrosshairConfig config,Entity target) {
        updateWorld();var client=MinecraftClient.getInstance();
        if(!(target instanceof LivingEntity living)||client.world==null||!holdsShield(living))return null;
        boolean blocking=living.isBlocking();
        return select(config,blocking,cooldowns.disabled(target.getUuid(),client.world.getTime(),blocking));
    }
    public static Integer select(CrosshairConfig config,boolean blocking,boolean disabled) {
        if(disabled&&config.shieldDisabledColorEnabled)return config.shieldDisabledColor;
        if(blocking&&config.shieldActiveColorEnabled)return config.shieldActiveColor;
        return null;
    }
}
