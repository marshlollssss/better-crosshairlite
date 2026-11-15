# Better Crosshairlite 1.0.0

Client-side custom crosshairs for **Minecraft 1.21.11 / Fabric**, with a built-in ReGlass-derived glass editor and Mod Menu support.

## Install

Use Java 21+, Fabric Loader 0.18.4+, and Fabric API for Minecraft 1.21.11. Put `build/libs/better-crosshairlite-1.21.11-1.0.0.jar` into your instance's `mods` folder, replacing earlier Better Crosshairlite jars. Mod Menu 17 is recommended. No separate ReGlass mod is needed.

Open **Mods → Better Crosshairlite → Configure**, or assign **Open Better Crosshairlite** in Controls. No server installation is required.

## Editor

Tabs: **Settings, Crosshair, Designer, Import**.

- Shape offers **Crosshair type: Cross, Circle, X, Import, Drawn**. Circle is a filled antialiased disk. Controls adapt to the selected type; cross/X support arm length, gap, thickness, individual arms and a center dot. All types support positional offsets.
- Appearance controls color, outline, opacity, rainbow speed and center-dot size. White is the default. Imported PNGs preserve their own colors.
- Settings controls enabling, item dynamics, dynamic amount, Show in F3, entity-target highlighting, and third-person/spectator visibility.
- Designer uses a centered 33×33 grid, with Pencil, Erase, Redo, Undo and Clear to the left, and brush/output size to the right. There is no preview on this tab. Left-drag draws; right-drag erases. No mirror controls are included. A stroke selects Drawn automatically; the type selector also selects an existing drawing.
- Import provides **Choose PNG**, a saved-PNG dropdown, image size, optional image outline and optional smoothing. Drop a PNG onto the game window as an alternative to the native chooser. Selecting a PNG automatically selects the Import type.

Changes save automatically while editing, and Done/Escape saves any pending changes before closing. The active drawing/import selection is saved with the configuration. The crosshair is hidden behind its editor to avoid duplicate previews. Disabling the mod's crosshair restores vanilla rendering.

## Imported PNG library

Imports are copied into `config/crosshairlite/imports/` as real PNG files. Filenames preserve the original PNG name (with unsupported filename characters replaced). Reimporting the same filename and bytes reuses the existing entry; different contents with the same name use a numbered suffix. Legacy hash suffixes are hidden in the dropdown. Older entries migrated under a generic imported name must be reimported from the original PNG to recover its original filename. The dropdown lists PNGs in that folder alphabetically and supports scrolling, arrow keys and Enter. PNGs manually placed in the folder appear when the tab is reopened.

Imports accept PNGs up to 4096×4096 and 16 MB, preserve alpha and aspect ratio, trim fully transparent margins automatically, and default to crisp resizing without an extra outline. Original source files may be moved or deleted after import. Existing embedded PNGs migrate into this library when settings are loaded. If a selected library file is missing or corrupt, the last embedded image remains as a fallback.

Importing adds a reusable library asset immediately; selecting an image saves and applies it automatically. The old path, trim, tint, remove and use-image controls are no longer shown.

## Dynamics

**Item dynamics** handles attack recovery independently of movement and reads held-item cooldowns (including ender pearls), bow/crossbow charge, trident/spear charge, and other item-use actions such as eating, drinking, blocking, brushing, spyglass and horn use. Charge/cooldown expansion shrinks toward resting size as the action completes. Continuous actions use a short animation window. Item use is no longer hidden by an editor setting.

**Dynamic amount** sets the maximum expansion in GUI units. Overlapping recovery and charging effects use the larger active effect rather than adding together. Set it to zero to remove expansion. Built-in shapes expand geometrically; PNG and Drawn types scale around the aim center. Show in F3 controls whether the custom crosshair remains visible while the debug overlay is open. F1 still hides the HUD normally.

## Build and verification

```sh
./gradlew build
./gradlew runClient
```

Distributable/source jars are in `build/libs`. Automated and runtime verification notes are in `VALIDATION.md`. The GUI uses continuous centered geometry and display-resolution antialiasing; drawings deliberately retain a pixel canvas. Other crosshair replacement mods may conflict. Full modpack compatibility is not claimed.

## Valorant crosshair mode

Set **Crosshair mode** to **Valorant** (or open the **Valorant** tab) to use a crosshair built like Valorant's.

- **General:** color (the eight Valorant presets or a custom hex), outlines (opacity and thickness), center dot (opacity and thickness), fade with firing error.
- **Inner lines / Outer lines:** show, opacity, thickness, length (with Valorant's split horizontal/vertical length), offset, movement error and firing error, each with its own multiplier.
- **Code:** paste a Valorant crosshair code to import it, or copy yours out. The Primary section is used; other sections (ADS, sniper) are kept so exporting does not drop them.
- Sizes are in **physical screen pixels**, like Valorant, so the crosshair does not grow with GUI scale. Lines are drawn as crisp pixel-aligned rectangles.
- **Movement error** follows how fast you move (full while airborne, halved while sneaking). **Firing error** spikes when you swing, attack, or release a charged item, then settles back. `ValorantDynamics` has the tuning constants (`MOVE_PX`, `FIRE_PX`, decay time).

Not carried over from Valorant: separate ADS and sniper crosshairs (Minecraft has no equivalent), and weapon-specific error values. Valorant's real error amounts depend on the gun, so Minecraft uses one fixed amount scaled by each multiplier.

## ReGlass attribution

The embedded renderer derives from [RedxAx/ReGlass](https://github.com/RedxAx/ReGlass), commit `352a67f8ccfbfaa709f60cc354d26ca4a5f3af78`, specialized for Minecraft 1.21.11. Its packages and shaders use Better Crosshairlite's namespace. Only rendering/API code is included; game-wide redesign, entrypoints, settings and keybindings are excluded. The glass pass runs only for this editor, with lazy GPU allocation and shutdown cleanup.

ReGlass is Copyright (c) 2025 ReStudio By RedxAx, MIT licensed. Its license is retained in `THIRD_PARTY_LICENSES/ReGlass-MIT.txt` and packaged under `META-INF/licenses/` in the jar.

## Target colors and control styling

**Crosshair → Target colors** has independent toggles and six-digit hex colors for hostile mobs, players and passive mobs (including neutral/non-monster mobs). Disabled categories retain the ordinary crosshair or rainbow color. Players are classified separately from mobs. Target coloring affects the alpha silhouette of imported PNGs while aimed at an enabled target category, then restores their original colors when aiming away. Each category defaults to off.

Buttons, sliders, dropdowns and color fields use a visible rounded outline. Sliders use a thin, beveled Minecraft-style track and handle centered in their row. Dropdown labels, options and color values are centered, and the preview displays the selected PNG's name.

### 1.4.3 simplification

The Presets tab and Movement dynamics option are removed. The tab order is Settings, Shape, Appearance, Designer, Import. Old movement flags in configuration files are ignored. The preview remains stationary even with Item dynamics enabled; actual item actions and attack recovery animate the gameplay crosshair. There is no idle breathing animation.

## 1.4.3 glass editor organization

Shape and Appearance are combined in **Crosshair**, with three sections: **Shape & position**, **Style**, and **Target colors**. Scroll within the control panel when the GUI scale leaves less vertical space; the preview stays fixed. Designer retains its centered canvas without a preview.

Controls and dropdowns use smooth shader-rendered glass edges rather than pixel-drawn borders. Dropdown lists and selected rows use rounded glass. Sliders use a six-GUI-unit glass track and a small rounded glass handle; the larger invisible input area remains easy to click or drag. The hard rectangular beveled handle and bottom progress strip have been removed.

### 1.4.3 glossy borders

Controls render on glass layers separate from their parent panel. An antialiased shader rim provides a persistent border with directional white highlights and a soft inner reflection. Slider handles and dropdown overlays use their own layers so their edges remain visible. No pixel-drawn border overlay is used.

### 1.4.3 source selector

The top of Settings has a **Crosshair mode** dropdown: **Default, Import, Drawn**. Default uses the built-in Cross/Circle/X shape configured in Crosshair → Shape & position. Import and Drawn no longer appear in that shape dropdown. Switching back to Default restores the last built-in shape. Editing the built-in shape while Import or Drawn is active does not switch the selected mode. Designer and Import tabs remain the editing/library tools for those modes.

### 1.4.3 slider height

Slider glass tracks now use the same full height and corner radius as normal buttons. Glossy borders and the rounded glass handle remain.

Changes save automatically. Toggle the editor in-game with backtick (`); rebind it in Settings or Minecraft Controls.

Targets includes optional active-shield (green) and disabled-shield (red) overrides with custom colors. Active means the target is blocking with a shield. Remote disable timing is estimated from a nearby shield-disable sound using the vanilla five-second cooldown; servers with custom cooldowns or suppressed sounds may differ. Shield colors take priority over player/mob colors. Settings includes a vanilla attack indicator toggle independent of crosshair dynamics.
