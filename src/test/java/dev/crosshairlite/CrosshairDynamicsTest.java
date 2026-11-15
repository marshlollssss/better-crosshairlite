package dev.crosshairlite;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CrosshairDynamicsTest {
    @Test void cooldownAndChargeShrinkToRestingSize() {
        assertEquals(1,CrosshairDynamics.remaining(0,20));assertEquals(0.5,CrosshairDynamics.remaining(10,20));
        assertEquals(0,CrosshairDynamics.remaining(20,20));assertEquals(0,CrosshairDynamics.remaining(200,20));
    }
    @Test void itemDynamicsDoNotDependOnLegacyMovementSetting() {
        var c=new CrosshairConfig();c.itemDynamics=true;c.dynamicAmount=8;
        c.dynamic=false;assertEquals(8,CrosshairDynamics.effective(c,0,1));assertEquals(4,CrosshairDynamics.effective(c,0,0.5));
        assertEquals(6,CrosshairDynamics.effective(c,0.75,0));
        c.dynamic=true;assertEquals(8,CrosshairDynamics.effective(c,0,1));
        c.itemDynamics=false;c.attackDynamics=false;assertEquals(0,CrosshairDynamics.effective(c,1,1));
    }
    @Test void attackAndItemCanBeEnabledIndependently() {
        var c=new CrosshairConfig();c.dynamicAmount=8;
        c.attackDynamics=false;c.itemDynamics=true;
        assertEquals(0,CrosshairDynamics.effective(c,1,0));assertEquals(8,CrosshairDynamics.effective(c,0,1));
        c.attackDynamics=true;c.itemDynamics=false;
        assertEquals(8,CrosshairDynamics.effective(c,1,0));assertEquals(0,CrosshairDynamics.effective(c,0,1));
    }
    @Test void idleDoesNotBreatheAndEffectsAreBounded() {
        var c=new CrosshairConfig();c.dynamicAmount=5;
        assertEquals(0,CrosshairDynamics.effective(c,0,0));assertEquals(5,CrosshairDynamics.effective(c,10,10));
        c.dynamicAmount=0;assertEquals(0,CrosshairDynamics.effective(c,1,1));
    }
}
