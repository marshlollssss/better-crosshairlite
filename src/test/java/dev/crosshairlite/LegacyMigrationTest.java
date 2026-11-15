package dev.crosshairlite;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMigrationTest {
    @TempDir Path root;
    @Test void preservesSettingsImportsAndCustomKeyAndDoesNotOverwriteNewSettings() throws Exception {
        Path config=Files.createDirectories(root.resolve("config"));
        Files.writeString(config.resolve("crosshair-studio.json"),"{\"opacity\":42}");
        Path imports=Files.createDirectories(config.resolve("crosshair-studio/imports"));
        Files.write(imports.resolve("test.png"),new byte[]{1,2,3});
        Files.writeString(root.resolve("options.txt"),"key_key.crosshair_studio.open:key.keyboard.k\n");
        LegacyMigration.migrate(config,root);
        assertEquals("{\"opacity\":42}",Files.readString(config.resolve("crosshairlite.json")));
        assertArrayEquals(new byte[]{1,2,3},Files.readAllBytes(config.resolve("crosshairlite/imports/test.png")));
        assertTrue(Files.readString(root.resolve("options.txt")).contains("key_key.crosshairlite.open:key.keyboard.k"));
        Files.writeString(config.resolve("crosshairlite.json"),"new settings");
        String options=Files.readString(root.resolve("options.txt"));
        LegacyMigration.migrate(config,root);
        assertEquals("new settings",Files.readString(config.resolve("crosshairlite.json")));
        assertEquals(options,Files.readString(root.resolve("options.txt")));
    }
}
