package dev.crosshairlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Old names are retained only here to preserve settings across the rename. */
final class LegacyMigration {
    static void migrate(Path configDir,Path gameDir) throws IOException {
        Path oldConfig=configDir.resolve("crosshair-studio.json");
        Path config=configDir.resolve("crosshairlite.json");
        if(Files.exists(oldConfig)&&!Files.exists(config))Files.copy(oldConfig,config);
        Path oldImports=configDir.resolve("crosshair-studio/imports");
        Path imports=configDir.resolve("crosshairlite/imports");
        if(Files.isDirectory(oldImports)) {
            Files.createDirectories(imports);
            try(var files=Files.list(oldImports)) {
                for(Path file:files.filter(Files::isRegularFile).toList()) {
                    Path target=imports.resolve(file.getFileName());
                    if(!Files.exists(target))Files.copy(file,target);
                }
            }
        }
        Path options=gameDir.resolve("options.txt");
        if(Files.exists(options)) {
            String text=Files.readString(options);
            if(!text.lines().anyMatch(line->line.startsWith("key_key.crosshairlite.open:"))) {
                String old=text.lines().filter(line->line.startsWith("key_key.crosshair_studio.open:")).findFirst().orElse(null);
                if(old!=null) {
                    String value=old.substring(old.indexOf(':')+1);
                    if(value.equals("key.keyboard.unknown"))value="key.keyboard.grave.accent";
                    Files.writeString(options,text+"\nkey_key.crosshairlite.open:"+value+"\n");
                }
            }
        }
    }
}
