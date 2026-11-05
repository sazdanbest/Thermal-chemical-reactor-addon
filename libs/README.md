# libs/ — third-party mod jars (not distributed with this repository)

This folder is where the mods we compile against are expected to live. The jars
themselves belong to their respective authors, are **not** part of this project and
are **not** distributed with this repository — they are ignored by git. You have to
download them yourself before the first build.

`build.gradle` resolves them from this folder through `flatDir`, so the **file names
must match exactly** (the coordinate is `name-version.jar`):

| File to put in `libs/` | Used for |
|---|---|
| `cofh_core-1.19.2-10.3.1.48.jar` | required (CoFH Core) |
| `thermal_foundation-1.19.2-10.3.1.57.jar` | required (Thermal Foundation) |
| `thermal_expansion-1.19.2-10.3.1.25.jar` | required (Thermal Expansion) |
| `thermal_core-1.19.2-10.3.0.9.jar` | required (Thermal Core — see note below) |
| `industrial-foregoing-1.19.2-3.3.2.3-5.jar` | bundled example recipe (`industrialforegoing:latex` / `dryrubber`) |
| `titanium-1.19.2-3.7.4-31.jar` | dependency of Industrial Foregoing |
| `jei-1.19.2-forge-11.8.1.1034.jar` | optional JEI integration |
| `kubejs-forge-1902.6.2-build.73.jar` | optional KubeJS integration |
| `rhino-forge-1902.2.3-build.284.jar` | dependency of KubeJS |
| `architectury-6.6.92-forge.jar` | dependency of KubeJS |

**Note on `thermal_core`:** Thermal ships its core library as a jarjar-nested jar
(`META-INF/jarjar/thermal_core-1.19.2-10.3.0.9.jar` inside `thermal_foundation.jar`).
Forge only unpacks it at runtime, so for compilation it has to be extracted manually:

```bash
unzip -j thermal_foundation-1.19.2-10.3.1.57.jar \
  'META-INF/jarjar/thermal_core-*.jar' -d libs/
```

**Note on `runClient`:** the dev runs use `run/`, and the mods above are compile-time
dependencies only. Mixing release jars directly into `run/mods/` is known to fail with
`NoSuchMethodError` — keep them in `libs/` and let `fg.deobf` handle them.
