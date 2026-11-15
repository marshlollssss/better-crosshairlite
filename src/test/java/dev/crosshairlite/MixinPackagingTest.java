package dev.crosshairlite;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MixinPackagingTest {
    @Test void entrypointsMustNotBeInsideReservedMixinPackage() throws Exception {
        ClassLoader loader=getClass().getClassLoader();
        try (var mixins=loader.getResourceAsStream("crosshairlite.mixins.json");
             var metadata=loader.getResourceAsStream("fabric.mod.json")) {
            assertNotNull(mixins);assertNotNull(metadata);
            var config=JsonParser.parseReader(new InputStreamReader(mixins)).getAsJsonObject();
            String reserved=config.get("package").getAsString()+".";
            var entries=JsonParser.parseReader(new InputStreamReader(metadata)).getAsJsonObject().getAsJsonObject("entrypoints");
            for(var stage:entries.entrySet()) for(var entry:stage.getValue().getAsJsonArray()) {
                String name=entry.getAsString();
                assertFalse(name.startsWith(reserved),name+" must be outside the Mixin package");
                assertNotNull(loader.getResource(name.replace('.','/')+".class"));
            }
            for(var mixin:config.getAsJsonArray("client"))
                assertNotNull(loader.getResource((reserved+mixin.getAsString()).replace('.','/')+".class"));
        }
    }
}
