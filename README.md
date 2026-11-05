# Chemical Reactor — Thermal Expansion Addon

| Minecraft | Forge | Mod version |
|---|---|---|
| 1.19.2 | 43.3.5 | 1.0.8 |

A Minecraft Forge addon for **Thermal Expansion** that adds one new machine — the **Chemical Reactor**.
It runs chemical reactions between fluids and items, and is built directly on top of the Thermal machine
framework, so it behaves like a native Thermal machine: RF energy, augments, side configuration,
redstone control and the usual Thermal GUI side panels all work out of the box.

* **Mod ID:** `thermal_addon_chemreactor`
* **Block / registry name:** `thermal_addon_chemreactor:chemical_reactor`
* **Display name:** EN "Chemical Reactor" / RU "Химический реактор"
* **Package / Maven group:** `com.sazdan.chemreactor`
* **Author:** sazdan
* **License:** MIT (see [`LICENSE`](LICENSE))

## Features

* **Fluid + item chemistry in one machine** — 2 input fluid tanks, 2 input item slots, 1 output fluid
  tank and 2 output item slots. Item inputs are order-independent; fluids are positional.
* **Chance-based outputs** — any output entry can carry a `chance` value; several entries of the same
  item are merged into a single output slot.
* **Native Thermal behaviour** — RF energy, all Thermal augments, redstone control, security and
  configuration side panels (the machine extends `cofh_core` / Thermal machine base classes).
* **Side configuration with face icons** — input/output icons are rendered on the actual block faces in
  the world (`ReconfigurableBakedModel`), including the machine's own front face in the config panel.
* **Recipe-driven fluid locking** — once a tank is filled, the partner tank only accepts the fluids that
  are paired with it in some recipe (prevents filling a tank with a fluid no recipe can consume).
* **Datapack recipes** — plain JSON under `data/<namespace>/recipes/chem_reactor/`.
* **Optional JEI integration** — a recipe category that mirrors the real machine GUI (soft dependency,
  the mod loads fine without JEI).
* **Optional KubeJS integration** — a recipe schema usable from `server_scripts` (soft dependency, the
  mod loads fine without KubeJS).

## Machine specification

| | Count | Capacity / amount |
|---|---|---|
| Input fluid tanks | 2 | 4000 mB each (tank 0 and tank 1, positional) |
| Input item slots | 2 | 1 stack per slot (order-independent) |
| Output fluid tank | 1 | 8000 mB |
| Output item slots | 2 | 1 stack per slot (one per distinct output item) |

* **Cycle time:** 100 ticks (5 s) at base speed, scaled by augments.
* **Energy:** 4000 FE per full cycle (~40 FE/tick at base speed).
* Output slots and the output tank reject manual insertion.

## Recipes

Recipes live in `data/<namespace>/recipes/chem_reactor/*.json`:

```json
{
  "type": "thermal_addon_chemreactor:chem_reactor",
  "ingredients": [
    { "fluid": "industrialforegoing:latex", "amount": 500 },
    { "fluid": "thermal:light_oil", "amount": 100 },
    { "item": "thermal:sulfur_dust" }
  ],
  "result": [
    { "item": "industrialforegoing:dryrubber" },
    { "fluid": "industrialforegoing:sludge", "amount": 100 }
  ],
  "energy": 4000
}
```

Limits per recipe (anything above them is rejected with a warning in the server log):

| | Inputs | Outputs |
|---|---|---|
| Items | up to 2 (order does not matter) | up to 2 |
| Fluids | up to 2 (positional: 1st → tank 0, 2nd → tank 1) | up to 1 |

At least one input and at least one output entry are required. The same JSON format is used by Thermal
machines (`MachineRecipeSerializer`), so recipes can also be generated from KubeJS:

```js
ServerEvents.recipes(event => {
    event.recipes.thermal_addon_chemreactor.chem_reactor([
        Fluid.of('industrialforegoing:latex', 500), // 1st fluid → tank 0
        Fluid.of('thermal:light_oil', 100),         // 2nd fluid → tank 1
        Item.of('thermal:sulfur_dust')              // item inputs are order-independent
    ], [
        Item.of('industrialforegoing:dryrubber'),   // output item slot
        Fluid.of('industrialforegoing:sludge', 100) // output tank
    ], 4000);                                       // FE per cycle
});
```

The bundled example recipe (`dryrubber`) also uses Industrial Foregoing fluids, which is why that mod is
listed as a dependency below — it is only needed for that one recipe, not for the machine itself.

## Requirements

Built and tested against **Minecraft 1.19.2 / Forge 43.3.5**.

| Mod | Version | Required |
|---|---|---|
| CoFH Core (`cofh_core`) | 10.3.1+ | yes |
| Thermal Core (`thermal`) | 10.3.0+ | yes |
| Thermal Foundation (`thermal_foundation`) | 10.3.1+ | yes |
| Thermal Expansion (`thermal_expansion`) | 10.3.1+ | yes |
| JEI (`jei`) | 11+ | no — adds the recipe category only |
| KubeJS (`kubejs`) | 1902.6+ | no — adds the recipe schema only |

## Installation

1. Install Minecraft Forge 43.3.5 for Minecraft 1.19.2.
2. Put the Thermal Series mods listed above into your `mods/` folder.
3. Put `thermal_addon_chemreactor-<version>.jar` into `mods/`.
4. Optionally add JEI and/or KubeJS.

The machine currently has **no crafting recipe of its own** — it is meant to be given by a modpack or a
KubeJS/datapack recipe:

```
/give @p thermal_addon_chemreactor:chemical_reactor
```

## Building from source

Requires **JDK 17** (Gradle 7.6.1 / ForgeGradle 5 do not run on newer JDKs).

The mods used for compilation are **not** shipped with this repository (they belong to their own
authors) — download them into `libs/` before the first build. Exact file names, versions and a note on
extracting `thermal_core` are in [`libs/README.md`](libs/README.md). Gradle resolves them from that folder
through `flatDir`, so no extra repositories are needed.

```bash
# point JAVA_HOME at a JDK 17 first, e.g.
#   export JAVA_HOME=/path/to/jdk-17
./gradlew build            # jar ends up in build/libs/
./gradlew runClient        # dev client (needs the mods from libs/ to be present in run/mods)
```

IDE setup:

```bash
./gradlew genIntellijRuns   # IntelliJ IDEA
./gradlew genEclipseRuns    # Eclipse
```

## Project layout

```
src/main/java/com/sazdan/chemreactor/
├── ChemReactorAddon.java              # @Mod entry point, registries, recipe manager hookup
├── block/ChemReactorBlock.java        # machine block
├── block/entity/ChemReactorTile.java  # machine logic (processing, tanks, slots, augments)
├── client/ChemReactorScreen.java      # GUI
├── client/compat/jei/                 # optional JEI plugin + recipe category
├── init/                              # deferred registers (blocks, items, tiles, menus,
│                                      #   recipe types, recipe serializers)
├── integration/kubejs/                # optional KubeJS plugin (loaded only if KubeJS is present)
├── inventory/ChemReactorContainer.java# menu / slot layout
└── recipe/                            # recipe + recipe manager
src/main/resources/                    # mods.toml, assets (models, textures, lang), data (recipes)
libs/                                  # third-party mod jars needed for compilation (see libs/README.md,
                                       #   the jars themselves are not tracked by git)
CLAUDE.md                              # detailed development notes (Russian)
```

## Known limitations

* The GUI uses the author's own background plus borrowed Thermal/CoFH sprites for tanks, energy bar,
  slots and the progress indicator. Final custom art is still to come.
* There is no crafting recipe for the machine block yet (see *Installation*).
* A few recent fixes (fluid locking, chance outputs, single-item recipes) are verified by a clean
  `./gradlew build` and await in-game verification on a real modpack server.

## Credits

* **Thermal Series / CoFH team** (King Lemming and contributors) — the machine framework this addon is
  built on.
* **Just Enough Items** (mezz and contributors) — optional recipe viewing integration.
* **KubeJS / Rhino / Architectury** (LatvianModder and contributors) — optional script integration.
* **Industrial Foregoing / Titanium** (Buuz135 and contributors) — used by the bundled example recipe.
* The Forge MDK files in the repository root (`README.txt`, `CREDITS.txt`, `changelog.txt`,
  `LICENSE-FORGE-MDK.txt`) come from the Minecraft Forge MDK and keep their original licenses. The mod
  jars in `libs/` belong to their respective authors as well — that is why they are not tracked by git.

## License

**MIT** — see [`LICENSE`](LICENSE). The mod's license field (`mod_license` in `gradle.properties`,
`license` in `src/main/resources/META-INF/mods.toml`) is set to `MIT` accordingly.

The MIT license covers this project's source code and assets only (everything under `src/`, the build
scripts and the documentation written for this project). It does **not** cover:

* the third-party mod jars used as compile dependencies — they are not distributed with this repository
  (see [`libs/README.md`](libs/README.md)) and each one stays under its own author's license;
* the Minecraft Forge MDK files in the repository root (`README.txt`, `CREDITS.txt`, `changelog.txt`,
  `LICENSE-FORGE-MDK.txt`), which keep their original licenses.

---

## Коротко по-русски

Аддон для **Thermal Expansion** (Minecraft 1.19.2, Forge 43.3.5), добавляющий механизм
**«Химический реактор»**: 2 входных бака по 4000 mB, 2 входных слота для предметов (порядок не важен),
выходной бак на 8000 mB и 2 выходных слота. Полная поддержка RF-энергии, аугментов, редстоун-управления
и конфигурации сторон Thermal; есть шансовые выходы, опциональная интеграция с JEI и KubeJS.
Рецепты — обычный JSON в `data/<namespace>/recipes/chem_reactor/` или скрипт KubeJS
`event.recipes.thermal_addon_chemreactor.chem_reactor(...)`. Своего крафта у блока пока нет —
выдаётся через `/give`. Автор мода — **sazdan**, лицензия — **MIT** (`LICENSE` в корне).
Джарники сторонних модов в `libs/` в репозиторий не входят и в git не отслеживаются — что и откуда
скачать, написано в `libs/README.md`.

