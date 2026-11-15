package dev.crosshairlite;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Retain disable events even when entity blocking metadata arrives a tick later. */
final class ShieldCooldowns {
    private static final class Cooldown {
        final long until;
        boolean sawLowered;
        Cooldown(long until){this.until=until;}
    }
    private final Map<UUID,Cooldown> entries=new HashMap<>();
    void clear(){entries.clear();}
    void disable(UUID id,long now){entries.values().removeIf(c->c.until<=now);entries.put(id,new Cooldown(now+100));}
    boolean disabled(UUID id,long now,boolean blocking) {
        var state=entries.get(id);
        if(state==null)return false;
        if(now>=state.until || (blocking&&state.sawLowered)){entries.remove(id);return false;}
        if(!blocking)state.sawLowered=true;
        return true;
    }
}
