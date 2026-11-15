package dev.crosshairlite;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;

public final class CrosshairDynamics {
    private CrosshairDynamics() {}
    public static int spread(CrosshairConfig c,ClientPlayerEntity player) {
        if(!c.itemDynamics&&!c.attackDynamics)return 0;
        double attack=1-player.getAttackCooldownProgress(0);
        double item=0;
        if(c.itemDynamics) {
            item=Math.max(cooldown(player,player.getMainHandStack()),cooldown(player,player.getOffHandStack()));
            if(player.isUsingItem()) {
                ItemStack active=player.getActiveItem();
                int duration=switch(active.getUseAction()) {
                    case BOW -> 20;
                    case CROSSBOW -> CrossbowItem.getPullTime(active,player);
                    case TRIDENT,SPEAR -> 10;
                    case BLOCK,SPYGLASS,BRUSH -> 40;
                    default -> Math.min(200,Math.max(1,active.getMaxUseTime(player)));
                };
                item=Math.max(item,remaining(player.getItemUseTime(),duration));
            }
        }
        return effective(c,attack,item);
    }
    private static double cooldown(ClientPlayerEntity player,ItemStack stack) {
        return stack.isEmpty()?0:player.getItemCooldownManager().getCooldownProgress(stack,0);
    }
    public static double remaining(double elapsed,double duration) {
        return Math.max(0,Math.min(1,1-elapsed/Math.max(1,duration)));
    }
    public static int effective(CrosshairConfig c,double attack,double item) {
        if(!c.itemDynamics&&!c.attackDynamics)return 0;
        return (int)Math.round(Math.max(0,c.dynamicAmount)*Math.max(c.attackDynamics?clamp(attack):0,c.itemDynamics?clamp(item):0));
    }
    private static double clamp(double value) { return Math.max(0,Math.min(1,value)); }
}
