package dev.crosshairlite;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Path;

public final class CrosshairLite implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Better Crosshairlite");
    public static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("crosshairlite.json");
    public static CrosshairConfig config = loadConfig();
    private static CrosshairConfig loadConfig() {
        try { LegacyMigration.migrate(CONFIG_PATH.getParent(),FabricLoader.getInstance().getGameDir()); }
        catch(java.io.IOException e) { LOGGER.error("Could not migrate legacy settings",e); }
        return CrosshairConfig.load(CONFIG_PATH);
    }
    public static KeyBinding open;
    @Override public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("crosshairlite","controls"));
        open = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.crosshairlite.open",InputUtil.Type.KEYSYM,GLFW.GLFW_KEY_GRAVE_ACCENT,category));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Options are loaded after mod initialization; migrate the old unbound default once.
            if(!config.editorKeyMigrated) {
                if(open.getBoundKeyTranslationKey().equals("key.keyboard.unknown")) {
                    open.setBoundKey(InputUtil.Type.KEYSYM.createFromCode(GLFW.GLFW_KEY_GRAVE_ACCENT));
                    KeyBinding.updateKeysByCode();client.options.write();
                }
                config.editorKeyMigrated=true;
                try { config.save(CONFIG_PATH); }
                catch(java.io.IOException e) { LOGGER.error("Could not save keybinding migration",e); }
            }
            while (open.wasPressed()) {
                if(client.currentScreen==null)client.setScreen(new CrosshairScreen(null));
            }
        });
    }
}
