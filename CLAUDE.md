# CLAUDE.md — thermal_addon_chemreactor

Контекст проекта для Claude Code. Читай этот файл перед началом работы в каждой новой сессии.

## О разработчике

Я не опытен в Java/разработке через нейросети. Объясняй код понятно, шаг за шагом.

**Формат работы:** генерируй код файл за файлом, по одному значимому шагу за раз.
После каждого этапа — проверка через `runClient` (запуск Minecraft из IntelliJ) перед тем как двигаться дальше.
Не выдавай весь проект целиком сразу.

## О проекте

Java-аддон для мода **Thermal Expansion** под Minecraft Forge. Добавляет новый механизм —
**"Химический реактор" (Chemical Reactor)** — полностью интегрированный в экосистему Thermal:
RF-энергия, аугменты, конфигурация сторон (вход/выход), редстоун-управление, общий стиль GUI.

- **Mod ID:** `thermal_addon_chemreactor`
- **Maven group/package:** `com.sazdan.chemreactor`
- **Автор:** sazdan
- **Версия мода:** 1.0.0
- **Registry name блока:** `thermal_addon_chemreactor:chemical_reactor`
- **Отображаемое имя:** EN "Chemical Reactor" / RU "Химический реактор"

## Окружение

| Параметр | Значение |
|---|---|
| Minecraft | 1.19.2 |
| Forge | 43.3.5 |
| Java | 17 (Amazon Corretto) |
| Gradle Wrapper | 7.6.1 |
| Маппинги | official (Mojang mappings) |
| IDE | IntelliJ IDEA Community |

Сервер, на котором будет использоваться мод, работает строго на Forge 43.3.5 — компилировать только под эту версию.

## Зависимости (libs/, ForgeGradle 5 / flatDir)

Локальные jar-файлы лежат в `libs/` и подключаются как deobf-зависимости:

- `cofh_core-1.19.2-10.3.1.48.jar`
- `thermal_foundation-1.19.2-10.3.1.57.jar`
- `thermal_expansion-1.19.2-10.3.1.25.jar`
- `thermal_core-1.19.2-10.3.0.9.jar` — **извлечён вручную** из
  `META-INF/jarjar/thermal_core-1.19.2-10.3.0.9.jar` внутри `thermal_foundation.jar` (см. ниже).

**Важная особенность build.gradle:** в ForgeGradle 5 через `flatDir` координаты deobf-зависимости
обязательно должны быть явно разделены на группу/имя/версию. Пустая группа ломает резолвер.

```groovy
repositories {
    flatDir { dir 'libs' }
}
dependencies {
    minecraft 'net.minecraftforge:forge:1.19.2-43.3.5'
    implementation fg.deobf("thermal:cofh_core:1.19.2-10.3.1.48")
    implementation fg.deobf("thermal:thermal_foundation:1.19.2-10.3.1.57")
    implementation fg.deobf("thermal:thermal_expansion:1.19.2-10.3.1.25")
    implementation fg.deobf("thermal:thermal_core:1.19.2-10.3.0.9")
}
```

**jarjar-ловушка (решено, см. "Текущий статус"):** `thermal_core` зашит внутрь `thermal_foundation.jar`
как jarjar-вложение. Forge разворачивает такие вложения только во время игры (рантайм-loader),
а не на этапе компиляции — поэтому Gradle не резолвит `cofh.thermal.lib.*` (пакет, который физически
лежит в `thermal_core`, а не в `thermal_foundation`), пока jar не извлечён вручную и не подключён
как обычная deobf-зависимость. Если будет апдейт версии Thermal — извлечение нужно повторить:
`unzip -p thermal_foundation-<ver>.jar META-INF/jarjar/thermal_core-<ver>.jar > libs/thermal_core-<ver>.jar`.

При необходимости могу предоставить в чате сами .jar файлы (cofh_core, thermal_expansion, thermal_foundation)
для анализа декомпилированного кода.

**Java для сборки:** в системе по умолчанию стоит JDK 25/26, который Gradle 7.6.1/ForgeGradle 5.1
не запускает ("Unsupported class file major version"). Собирать с `JAVA_HOME`, указывающим на
закэшированный Gradle'ом Temurin 17 (например
`C:\Users\user\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.19+10`), либо настроить
системный JDK 17 как Project SDK в IntelliJ.

**Маппинги — official, не SRG:** код должен использовать официальные имена методов/полей
(`registerDefaultState`, `getStateForPlacement`, `newBlockEntity`, `isClientSide` и т.п.), а не
SRG-имена вида `m_49959_`/`f_46443_`. SRG-имена существуют только внутри внутреннего
ремаппинг-пайплайна Forge и не компилируются против official-маппированного jar'а Minecraft —
такая ошибка уже случалась в `ChemReactorBlock`/`ChemReactorTile` и была исправлена переименованием
1:1 (сверено по `forge-1.19.2-43.3.5_mapped_official_1.19.2-sources.jar` из кэша Gradle).

## mods.toml

- Шаблонный пакет `com.example.examplemod` (включая `ExampleMod.java`, `Config.java`) — **удалён полностью**,
  чтобы не было конфликта метаданных.
- Обязательные зависимости: `forge`, `minecraft`, `cofh_core`, `thermal_foundation`, `thermal_expansion`,
  `thermal` (modId встроенного thermal_core).

## Архитектура (на основе анализа декомпилированного Thermal Expansion)

Каждая машина в Thermal построена по единому шаблону:

- **Tile (BlockEntity)** — наследуется от `cofh.thermal.lib.block.entity.MachineBlockEntity`.
  Этот базовый класс уже реализует RF-энергию, аугменты, конфигурацию сторон, редстоун.
  Наследник переопределяет только: конструктор, `getBaseProcessTick()`, `cacheRecipe()`,
  `validateInputs()`, `validateOutputs()`.
- **Container** — расставляет слоты/баки в GUI, минимум кода.
- **Screen** — визуальная раскладка GUI (прогресс-бар, баки, слоты), минимум кода.
- **Рецепты** — отдельные `RecipeType` + `RecipeSerializer`, через JSON в `data/<modid>/recipes/`.
  Система должна быть гибкой и расширяемой — новые рецепты добавляются только через JSON,
  без изменения Java-кода.

**Ближайший существующий аналог в Thermal** — Brewer (`MachineBrewerTile`, `MachineBrewerContainer`,
`MachineBrewerScreen`): принимает 1 жидкость + 1 предмет на входе, выдаёт 1 жидкость.
Chemical Reactor — расширенная версия этой схемы.

### Классы проекта

- `ChemReactorBlock` — наследует `Block`, реализует `EntityBlock`. Свойство `BlockStateProperties.HORIZONTAL_FACING`.
- `ChemReactorTile` — наследует `MachineBlockEntity` (CoFH).
- Регистрация: `ChemReactorBlocks`, `ChemReactorItems`, `ChemReactorBlockEntities`.

## Спецификация механизма "Chemical Reactor"

**Входы:**
- 2 бака для жидкостей, по 4000 mB каждый
- 2 слота для предметов

**Выходы:**
- 1 бак для жидкости, 8000 mB
- 2 слота для предметов

**Обработка:**
- Время цикла без ускорителей: 40 секунд (800 тиков)
- Потребление энергии: 4000 FE за полный цикл (~5 FE/тик при базовой скорости)
- Полная поддержка аугментов Thermal "из коробки" через наследование от `MachineBlockEntity`

**Первый тестовый рецепт:**
- Вход: 500 mB `industrialforegoing:latex` + 500 mB `thermal:light_oil` + 1x `thermal:sulfur_dust`
- Выход: 1x `industrialforegoing:dryrubber`

(Сначала будут заданы 500 mB для теста рецепта — финальные объёмы баков 4000/4000/8000 mB,
это разные параметры: ёмкость бака ≠ объём в рецепте.)

## Ресурсы и текстуры

- Модель блока: `minecraft:block/orientable` (автоповорот лицевой панели по FACING)
- Корпус (верх/низ/бока): общие текстуры машин Thermal —
  `thermal:block/machines/machine_top`, `machine_side`, `machine_bottom`
- Лицевая панель (front, статика): `chemical_reactor.png`
- Лицевая панель (активное состояние): `chemical_reactor_active.png` — спрайт 6 кадров по вертикали,
    + `.mcmeta` с анимацией (интерполяция включена, `frametime: 8`)
- Lang-файлы: `en_us`, `ru_ru`

## Текущий статус

**Готово и проверено:**
- Окружение настроено: JDK 17 выбран как Project SDK, Forge MDK 43.3.5 открыт как проект, `genIntellijRuns` выполнен успешно
- Thermal jar'ы декомпилированы и проанализированы
- Блок регистрируется, корректно загружается, использует текстуры корпуса Thermal, есть заглушка лицевой панели
- Блок поворачивается к игроку при установке
- **Шаг 3 (BlockEntity) сделан и проверен в игре:** `ChemReactorTile extends MachineBlockEntity` компилируется
  и работает — блок ставится, поворачивается к игроку, создаёт тайл, тикает на сервере, слоты
  (2 input/2 output) и баки (2×4000 mB input, 1×8000 mB output) настроены. GUI пока не открывается
  (`createMenu` возвращает `null`) — это ожидаемо, экран будет на шаге 6. Крашей нет.

**🟢 Блокер шага 3 решён.** Было два независимых наслаивающихся бага:

1. `cofh.thermal.lib.*` не резолвился, потому что `thermal_core` зашит внутрь `thermal_foundation.jar`
   через jarjar (`META-INF/jarjar/thermal_core-1.19.2-10.3.0.9.jar`) — Forge разворачивает такие
   вложения только в рантайме, не на этапе компиляции. Исправлено извлечением jar'а в `libs/` и
   добавлением отдельной deobf-зависимости (см. "Зависимости" выше).
2. После этого вскрылась вторая, не связанная проблема: `ChemReactorBlock.java`/`ChemReactorTile.java`
   были написаны с использованием SRG-имён (`m_49959_`, `f_61374_`, `m_7208_` и т.п.) вместо
   official-маппингов, из-за чего методы-переопределения (`newBlockEntity`, `createMenu`,
   `createBlockStateDefinition` и др.) не распознавались компилятором как override. Исправлено
   переименованием 1:1 в official-имена (см. "Маппинги — official, не SRG" выше).

## Связанные, но отдельные задачи (НЕ трогать в рамках этого аддона)

В модпаке уже используется KubeJS + аддон kubejs-thermal для модификации рецептов существующих
машин Thermal (переименование Brewer, замена жидкости latex). Эта работа уже завершена и не
относится к текущему Java-аддону — не путать и не пересекать с этой задачей.

## План разработки

1. ✅ Настройка проекта — build.gradle, mods.toml, зависимости в libs/
2. ✅ Регистрация блока и предмета
3. ✅ BlockEntity (Tile) — `MachineBlockEntity` резолвится, компилируется, проверено в игре
4. ⬜ Система рецептов — тип `chemreactor`, сериализатор, менеджер, первый рецепт
   (проверку в игре после этого шага, как и после всех остальных, разработчик всегда делает сам —
   не запускай `runClient` самостоятельно, просто сообщай, что готово к проверке)
5. ⬜ Container — слоты и баки для GUI
6. ⬜ Screen (GUI) — прогресс-бар, баки
7. ⬜ Assets — blockstate, модели блока/предмета, lang (en_us, ru_ru), текстуры
8. ⬜ Сборка и тест — Gradle build, jar в модпак, проверка в игре