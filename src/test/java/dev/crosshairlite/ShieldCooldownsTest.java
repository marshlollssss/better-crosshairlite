package dev.crosshairlite;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShieldCooldownsTest {
    @Test void staleBlockingMetadataDoesNotEraseDisableEvent() {
        var tracker=new ShieldCooldowns();var id=UUID.randomUUID();
        tracker.disable(id,20);
        assertTrue(tracker.disabled(id,20,true));
        assertTrue(tracker.disabled(id,21,true));
        assertTrue(tracker.disabled(id,22,false));
        assertTrue(tracker.disabled(id,119,false));
        assertFalse(tracker.disabled(id,120,false));
    }
    @Test void ActuallyRaisingShieldAgainEndsEstimateEarly() {
        var tracker=new ShieldCooldowns();var id=UUID.randomUUID();tracker.disable(id,0);
        assertTrue(tracker.disabled(id,1,false));
        assertFalse(tracker.disabled(id,40,true));
    }
    @Test void worldChangeAndOtherPlayersDoNotRetainCooldown() {
        var tracker=new ShieldCooldowns();var id=UUID.randomUUID();tracker.disable(id,0);
        assertFalse(tracker.disabled(UUID.randomUUID(),1,false));
        tracker.clear();assertFalse(tracker.disabled(id,1,false));
    }
}
