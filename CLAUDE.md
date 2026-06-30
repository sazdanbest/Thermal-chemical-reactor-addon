# CLAUDE.md — thermal_addon_chemreactor

Контекст проекта для Claude Code. Читай этот файл перед началом работы в каждой новой сессии.

## О разработчике

Я не опытен в Java/разработке через нейросети. Объясняй код понятно, шаг за шагом.

**Формат работы:** генерируй код файл за файлом, по одному значимому шагу за раз.
Не выдавай весь проект целиком сразу.

**Запуск `runClient`:** перед каждым запуском `runClient` сначала спроси меня в чате текстом,
можно ли его запускать. Запускай только после того, как я отвечу "да". Сам, без вопроса,
`runClient` не запускай — даже если код только что собрался без ошибок.

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
- `industrial-foregoing-1.19.2-3.3.2.3-5.jar`, `titanium-1.19.2-3.7.4-31.jar` — нужны для первого
  тестового рецепта (`industrialforegoing:latex`/`dryrubber`). **Важно: не клади release-моды
  в `run/mods/` напрямую** — см. "Грабли: моды в run/mods падают с NoSuchMethodError" ниже,
  только через `fg.deobf` в `libs/`, как остальные.

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
    implementation fg.deobf("industrialforegoing:industrial-foregoing:1.19.2-3.3.2.3-5")
    implementation fg.deobf("titanium:titanium:1.19.2-3.7.4-31")
}
```

**Грабли: моды в `run/mods/` падают с NoSuchMethodError/NoSuchFieldError.** Сначала Industrial
Foregoing и Titanium были просто положены как обычные jar'ы в `run/mods/` — игра падала при
загрузке модов: `NoSuchMethodError: DyeColor.m_41060_()` (Industrial Foregoing) и
`NoSuchFieldError: f_42799_` (Titanium, в их собственном debug-классе
`_impl.test.recipe.TestSerializableRecipe`). Причина: релизные jar'ы любых Forge-модов содержат
"сырые" SRG-имена методов/полей (так компилируются все моды), а наша dev-среда (`runClient` с
official-маппингами) их не понимает — в продакшене Forge сам ремапит SRG на лету, а тут такого
шага для содержимого `run/mods/` нет. Признак в краш-репорте: у успешно загруженных модов в имени
jar добавлен суффикс `_mapped_official_1.19.2` (это и есть прошедшие деобфускацию через `fg.deobf`
файлы из `libs/`), а у упавших — нет. **Решение — то же самое, что мы уже делаем для cofh_core и
thermal_*: класть jar в `libs/` и подключать через `fg.deobf`, а не в `run/mods/`.** После этого
оба мода грузятся нормально.

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
  Также переопределяет `use()` — открывает GUI через `NetworkHooks.openScreen(...)` (см. ниже).
- `ChemReactorTile` — наследует `MachineBlockEntity` (CoFH). Переопределяет только `getBaseProcessTick()`
  (фиксировано 800 тиков, не зависит от менеджера рецептов) и `cacheRecipe()` (ищет рецепт через
  `ChemReactorRecipeManager`). `validateInputs()`/`validateOutputs()`/`processStart()`/`processFinish()`
  НЕ переопределяются — у `MachineBlockEntity` уже есть универсальная реализация по числу слотов/баков
  (проверено на примере `MachineBottlerTile`, у которой тоже выход только предметом).
- Регистрация: `ChemReactorBlocks`, `ChemReactorItems`, `ChemReactorBlockEntities`,
  `ChemReactorRecipeTypes`, `ChemReactorRecipeSerializers`, `ChemReactorMenus`.

### Рецепты (шаг 4, готово)

- `recipe/ChemReactorRecipe.java` — `extends cofh.thermal.lib.util.recipes.ThermalRecipe` (как `BrewerRecipe`
  у Thermal), только данные рецепта (вход/выход/энергия), без логики.
- `init/ChemReactorRecipeTypes.java` / `init/ChemReactorRecipeSerializers.java` — регистрация `RecipeType`/
  `RecipeSerializer` через готовые классы CoFH (`cofh.lib.util.recipes.SerializableRecipeType`,
  `cofh.thermal.lib.util.recipes.MachineRecipeSerializer`) — свой JSON-парсер писать не пришлось,
  `MachineRecipeSerializer` уже умеет читать смешанный список `"ingredients"` (предметы + жидкости вперемешку).
- `recipe/ChemReactorRecipeManager.java` — синглтон-менеджер (по образцу `BottlerRecipeManager`/
  `PressRecipeManager` из Thermal), ищет рецепт по содержимому 2 предметных слотов + 2 баков.
  **Важно: совпадение позиционное**, как и у самих машин Thermal с несколькими входами (см.
  `PressRecipeManager`, слот "die" = индекс 1) — НЕ по множеству/любой комбинации. Для текущего
  рецепта это значит: `sulfur_dust` должен лежать в предметном слоте 0 (слот 1 не используется),
  `latex` — в баке 0, `light_oil` — в баке 1 (порядок = порядок объявления в JSON `"ingredients"`).
  Если переставить жидкости местами по бакам — рецепт не найдётся. При добавлении новых рецептов
  с другим набором входов это стоит держать в голове.
- Подключение к Thermal: `ThermalRecipeManagers.registerManager(ChemReactorRecipeManager.instance())`
  в конструкторе `ChemReactorAddon` — публичный API CoFH специально для аддонов, Thermal сам вызывает
  `refresh()` у всех зарегистрированных менеджеров при (пере)загрузке рецептов.
- **Грабли:** `RecipeManager.byType(RecipeType)` в official-маппингах **private** (можно проверить по
  `forge-...-sources.jar` из кэша Gradle) — внешний код вызвать его не может. Используем публичный
  аналог `recipeManager.getAllRecipesFor(RecipeType)` (возвращает `List<T>` вместо `Map`), без
  access-transformer'а.
- JSON-рецепт: `data/thermal_addon_chemreactor/recipes/chem_reactor/dryrubber.json`.

### GUI (шаги 5-6, готово, но без своих текстур)

- `inventory/ChemReactorContainer.java` — `extends cofh.core.inventory.container.TileCoFHContainer`
  (НЕ голый `AbstractContainerMenu`) — это даёт бесплатно `quickMoveStack` (шифт-клик) и `stillValid`
  (закрытие при разрушении блока/уходе игрока), писать их с нуля не пришлось. 4 предметных слота
  (`SlotCoFH` + `InvWrapperCoFH` вокруг `tile.getItemInv()`) + стандартный инвентарь игрока через
  `bindPlayerInventory()`. Аугмент-слоты НЕ добавлены — `ChemReactorTile` пока не вызывает
  `addAugmentSlots(...)` в конструкторе (см. "Известные пробелы" ниже).
- `init/ChemReactorMenus.java` — регистрация `MenuType` через `IForgeMenuType.create(...)`,
  клиентская фабрика берёт `Level`/`Player` через `cofh.core.util.ProxyUtils.getClientWorld()/
  getClientPlayer()` (тот же приём, что и у `TExpContainers` в самом Thermal).
- `client/ChemReactorScreen.java` — `extends AbstractContainerScreen<ChemReactorContainer>` (ванильный,
  НЕ `cofh.thermal.lib.client.gui.MachineScreen` — тот требует кастомную PNG-текстуру панели, которой
  у нас пока нет). Фон/баки/прогресс рисуются примитивами через `fill()`, а не `blit()` готовой
  картинки. Баки красятся настоящим тинтом жидкости через `IClientFluidTypeExtensions`.
  Зарегистрирован в `ChemReactorAddon` через `FMLClientSetupEvent` + `MenuScreens.register(...)`
  (НЕ `RegisterMenuScreensEvent` — этот класс появился только в Forge для MC 1.20+, в 1.19.2 его нет).
- **Критичная деталь, без которой GUI физически не открывался бы даже с правильным `createMenu()`:**
  нужно было добавить `ChemReactorBlock.use(...)`, вызывающий
  `NetworkHooks.openScreen((ServerPlayer) player, menuProvider, pos)`. Без этого override клик по
  блоку вообще ничего не делает — `createMenu()` у тайла никто не вызывает.
- Проверено в игре: интерфейс открывается по правому клику.

**Известные пробелы (сознательно отложено, не баг):**
- Аугмент-слоты не добавлены ни в тайл, ни в GUI (`addAugmentSlots`/`bindAugmentSlots` не вызываются) —
  хотя по спецификации полная поддержка аугментов заявлена "из коробки". Нужно отдельно: вызвать
  `this.addAugmentSlots(ThermalCoreConfig.machineAugments)` в конструкторе `ChemReactorTile` и
  `bindAugmentSlots(tileInv, 4, tile.augSize())` в контейнере + позиционирование слотов на экране.
- Charge-слот (зарядка энергией через предмет) не добавлен в `inventory` тайла — Brewer/Bottler его
  добавляют (`this.inventory.addSlot(this.chargeSlot, StorageGroup.INTERNAL)`), мы — нет.
- Своих текстур для GUI (фон, баки, прогресс) нет — экран рисуется примитивами (`fill()`), не PNG.
  Когда появится арт (см. "Ресурсы и текстуры"), `ChemReactorScreen` нужно переписать под `blit()`.
- Полоска энергии (RF) на экране не отображается.

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
  (2 input/2 output) и баки (2×4000 mB input, 1×8000 mB output) настроены. Крашей нет.
- **Шаг 4 (рецепты) сделан** — `ChemReactorRecipe`/`ChemReactorRecipeManager`/`ChemReactorRecipeTypes`/
  `ChemReactorRecipeSerializers` + тестовый JSON-рецепт dryrubber. Подробности и грабли — см.
  "Рецепты (шаг 4, готово)" выше. Не проверено в игре отдельно (рецепт зависит от Industrial
  Foregoing — latex/dryrubber; сама загрузка/компиляция рецепта проверена, полный цикл обработки
  с реальными предметами в инвентаре — ещё нет).
- **Шаги 5-6 (Container + Screen) сделаны и проверены в игре:** интерфейс открывается по правому
  клику, без своей текстуры (примитивы вместо PNG). Подробности и осознанные пробелы (аугменты,
  charge-слот, полоска энергии) — см. "GUI (шаги 5-6, готово, но без своих текстур)" выше.
- **Industrial Foregoing + Titanium подключены и работают** как deobf-зависимости в `libs/`
  (изначально лежали как обычные jar'ы в `run/mods/` и крашились — см. "Грабли: моды в run/mods..."
  в разделе "Зависимости").

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
4. ✅ Система рецептов — тип `chem_reactor`, сериализатор, менеджер, тестовый рецепт dryrubber
5. ✅ Container — слоты и баки для GUI (`ChemReactorContainer`, без аугмент-слотов)
6. ✅ Screen (GUI) — открывается в игре; фон/баки/прогресс примитивами, без своих текстур
7. ⬜ Assets — blockstate, модели блока/предмета, lang (en_us, ru_ru) уже есть (см. "Ресурсы и
   текстуры"); **не хватает**: текстур самого GUI (фон/баки/прогресс — сейчас заглушка из `fill()`)
8. ⬜ Сборка и тест — Gradle build, jar в модпак, проверка в игре

**Дальше имеет смысл (не пронумеровано строго, на усмотрение):**
- Проверить полный цикл обработки тестового рецепта в игре (нужны fluid-баки с latex/light_oil
  и sulfur_dust в предметных слотах — позиционно, см. "Рецепты" выше).
- Аугмент-слоты: добавить `addAugmentSlots(...)` в `ChemReactorTile` + `bindAugmentSlots(...)` в
  `ChemReactorContainer` + позиционирование в `ChemReactorScreen` (сейчас полностью отсутствуют).
- Charge-слот (зарядка аугментов/энергии предметом) — не добавлен в инвентарь тайла.
- Своя текстура GUI (фон/баки/прогресс/энергия) — после готовности арта переписать
  `ChemReactorScreen` под `blit()` вместо `fill()`.
- Полоска энергии (RF) на экране.
- (После 8) — упаковать jar и закинуть в реальный модпак на сервере (Forge 43.3.5).

**Перед очисткой/компактификацией сессии:** весь контекст по сделанному за эту сессию (jarjar/SRG
фикс, рецепты, GUI, IF/Titanium фикс) уже перенесён сюда максимально подробно — этот файл должен
быть единственным источником правды, восстанавливать ничего из истории чата не нужно.