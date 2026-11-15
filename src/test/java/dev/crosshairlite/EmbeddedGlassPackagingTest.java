package dev.crosshairlite;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EmbeddedGlassPackagingTest {
    @Test void standaloneMetadataAndShadersArePresent() throws Exception {
        ClassLoader loader=getClass().getClassLoader();
        try(var stream=loader.getResourceAsStream("fabric.mod.json")) {
            assertNotNull(stream);
            var metadata=JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject();
            assertFalse(metadata.getAsJsonObject("depends").has("reglass"));
            assertEquals(1,metadata.getAsJsonObject("entrypoints").getAsJsonArray("client").size());
        }
        for(String shader:new String[]{"core/blit_fullscreen.vsh","program/blur.fsh","program/liquid_glass_gui.fsh"})
            assertNotNull(loader.getResource("assets/crosshairlite/shaders/"+shader));
        assertNull(loader.getResource("restudio/reglass/client/ReGlassClient.class"));
    }
    @Test void onlyRenderingHooksAreIncluded() throws Exception {
        try(var stream=getClass().getClassLoader().getResourceAsStream("crosshairlite.mixins.json")) {
            assertNotNull(stream);
            var mixins=JsonParser.parseReader(new InputStreamReader(stream)).getAsJsonObject().getAsJsonArray("client");
            var allowed=java.util.Set.of("InGameHudMixin","ShieldSoundMixin","glass.accessor.DrawContextAccessor","glass.accessor.GameRendererAccessor",
                "glass.accessor.GuiRendererAccessor","glass.accessor.GuiRenderStateAccessor","glass.client.GuiRendererMixin",
                "glass.widgets.GuiRendererMixin","glass.logical.GameRendererMixin");
            assertEquals(allowed.size(),mixins.size());
            for(var mixin:mixins) assertTrue(allowed.contains(mixin.getAsString()),mixin.toString());
        }
    }
}
