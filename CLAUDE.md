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
| Java | 17 (Amazon Corretto / Temurin) |
| Gradle Wrapper | 7.6.1 |
| Маппинги | official (Mojang mappings) |
| IDE | IntelliJ IDEA Community |

Сервер, на котором будет использоваться мод, работает строго на Forge 43.3.5 — компилировать
только под эту версию.

**Java для сборки:** в системе по умолчанию стоит JDK 25/26, который Gradle 7.6.1/ForgeGradle 5.1
не запускает ("Unsupported class file major version"). Собирать с `JAVA_HOME`, указывающим на
закэшированный Gradle'ом Temurin 17 (например
`C:\Users\user\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.19+10`), либо настроить
системный JDK 17 как Project SDK в IntelliJ. Пример команды сборки:

```
JAVA_HOME="C:\Users\user\.gradle\jdks\eclipse_adoptium-17-amd64-windows\jdk-17.0.19+10" ./gradlew compileJava
```

**Маппинги — official, не SRG:** код должен использовать официальные имена методов/полей
(`registerDefaultState`, `getStateForPlacement`, `newBlockEntity`, `isClientSide` и т.п.), а не
SRG-имена вида `m_49959_`/`f_46443_`. SRG-имена существуют только внутри внутреннего
ремаппинг-пайплайна Forge и не компилируются против official-маппированного jar'а Minecraft.

## Зависимости (libs/, ForgeGradle 5 / flatDir)

Локальные jar-файлы лежат в `libs/` и подключаются как deobf-зависимости:

- `cofh_core-1.19.2-10.3.1.48.jar`
- `thermal_foundation-1.19.2-10.3.1.57.jar`
- `thermal_expansion-1.19.2-10.3.1.25.jar`
- `thermal_core-1.19.2-10.3.0.9.jar` — **извлечён вручную** из
  `META-INF/jarjar/thermal_core-1.19.2-10.3.0.9.jar` внутри `thermal_foundation.jar` (см. ниже).
- `industrial-foregoing-1.19.2-3.3.2.3-5.jar`, `titanium-1.19.2-3.7.4-31.jar` — нужны для первого
  тестового рецепта (`industrialforegoing:latex`/`dryrubber`). **Не класть release-моды в
  `run/mods/` напрямую** — см. "Грабли: моды в run/mods падают с NoSuchMethodError" ниже, только
  через `fg.deobf` в `libs/`, как остальные.

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

**Грабли: моды в `run/mods/` падают с NoSuchMethodError/NoSuchFieldError.** Релизные jar'ы любых
Forge-модов содержат "сырые" SRG-имена методов/полей, а наша dev-среда (`runClient` с
official-маппингами) их не понимает — в продакшене Forge сам ремапит SRG на лету, а для содержимого
`run/mods/` такого шага нет. Признак в краш-репорте: у успешно загруженных модов в имени jar
добавлен суффикс `_mapped_official_1.19.2` (прошли деобфускацию через `fg.deobf` из `libs/`), а у
упавших — нет. **Решение — класть jar в `libs/` и подключать через `fg.deobf`, а не в `run/mods/`.**

**jarjar-ловушка (решено):** `thermal_core` зашит внутрь `thermal_foundation.jar` как
jarjar-вложение. Forge разворачивает такие вложения только во время игры (рантайм-loader), а не на
этапе компиляции — поэтому Gradle не резолвит `cofh.thermal.lib.*` (физически лежит в
`thermal_core`), пока jar не извлечён вручную и не подключён как обычная deobf-зависимость. Если
будет апдейт версии Thermal — извлечение нужно повторить:
`unzip -p thermal_foundation-<ver>.jar META-INF/jarjar/thermal_core-<ver>.jar > libs/thermal_core-<ver>.jar`.

**Декомпиляция Thermal/CoFH для справки:** в кэше Gradle лежит ForgeFlower
(`~/.gradle/caches/forge_gradle/maven_downloader/net/minecraftforge/forgeflower/...`), а
деобфусцированные (official-mapped, но ещё не декомпилированные) jar'ы — в
`~/.gradle/caches/forge_gradle/deobf_dependencies/thermal/<artifact>/<version>_mapped_official_1.19.2/`.
Команда декомпиляции произвольного jar'а:
`java -jar forgeflower-*.jar -din=1 -rbr=1 -dgs=1 -asc=1 <mapped.jar> <out.jar>` — на выходе jar с
`.java`-исходниками, можно `unzip -p out.jar path/To/Class.java`. Этим способом за эту сессию было
найдено устройство почти всех ключевых механизмов CoFH (см. разделы ниже) — при новых вопросах
"как это делает настоящий Thermal" начинать стоит отсюда, а не гадать.

## mods.toml

- Шаблонный пакет `com.example.examplemod` — **удалён полностью**.
- Обязательные зависимости: `forge`, `minecraft`, `cofh_core`, `thermal_foundation`,
  `thermal_expansion`, `thermal` (modId встроенного thermal_core).

## Архитектура

Каждая машина в Thermal построена по единому шаблону, и Chemical Reactor следует ему буквально —
**во всех точках, где это возможно, используются готовые базовые классы CoFH/Thermal, а не
переизобретается своя логика.** Ближайший существующий аналог — Brewer (`MachineBrewerTile`,
`MachineBrewerContainer`, `MachineBrewerScreen`): 1 жидкость + 1 предмет на входе → 1 жидкость на
выходе. Chemical Reactor — расширенная версия этой схемы (2 жидкости + 2 предмета на входе → 1
жидкость + 2 предмета на выходе).

### ChemReactorBlock

`extends cofh.core.block.TileBlockActive4Way` (→ `TileBlockActive` → `TileBlock4Way`/
`TileBlockCoFH`). У Thermal сами машины (включая Brewer) — буквально `new TileBlockActive4Way(...)`
в `TExpBlocks`, без собственного подкласса; свой `ChemReactorBlock` существует только чтобы дать
классу имя. **Собственных переопределений методов нет вообще** — конструктор только пробрасывает
`Properties`/`tileClass`/`blockEntityType` в `super(...)`. Базовый класс даёт бесплатно:

- свойства состояния `FACING_HORIZONTAL` (4 стороны) + `ACTIVE` (вкл/выкл текстура);
- `use()` — открытие GUI, гаечный ключ (поворот/демонтаж), проверка Security-доступа, и
  взаимодействие с ведром/жидкостной ячейкой в руке (`TileCoFH.onActivatedDelegate()` пробует
  влить/слить через `IFluidHandler`-капабилити тайла ПЕРЕД открытием GUI; если жидкость не подходит
  под `validFluid()` — просто открывается GUI, как обычный клик);
- `onRemove()` — вызывает `tile.onReplaced(...)`, из-за чего при поломке блока выпадают предметы и
  аугменты (`AugmentableBlockEntity.onReplaced()` это умеет, учитывая
  `ThermalCoreConfig.keepItems`/`keepAugments`); жидкости при этом просто исчезают вместе с тайлом —
  так и задумано;
- `neighborChanged()` — пробрасывает изменение редстоун-сигнала в `tile.neighborChanged()`, без
  этого `redstoneControl.power` не обновлялся бы после первой установки блока.

**Регистрация** (`ChemReactorBlocks`): `new ChemReactorBlock(Properties..., ChemReactorTile.class,
ChemReactorBlockEntities.CHEMICAL_REACTOR::get)` — третий параметр именно как **method reference**
(`::get`), не сам `RegistryObject` напрямую (типизация `Supplier<BlockEntityType<?>>` требует
ковариантного возврата, который даёт только method reference). `ChemReactorBlocks` и
`ChemReactorBlockEntities` ссылаются друг на друга (блоку нужен `BlockEntityType`,
`BlockEntityType.Builder.of(...)` нужен блок для `validBlocks`) — это безопасно, обе стороны
обёрнуты в ленивые лямбды/суплаеры и разрешаются только во время самого события регистрации.

### ChemReactorTile

`extends cofh.thermal.lib.block.entity.MachineBlockEntity` (→ `Reconfigurable4WayBlockEntity` →
`AugmentableBlockEntity` → `TileCoFH`). Даёт бесплатно: RF-энергию, аугменты, редстоун-контроль,
конфигурацию сторон блока, позиционный поиск рецепта через `IMachineInventory`.

Конструктор настраивает инвентарь тайла **строго в этом порядке** (порядок важен — определяет
сырые индексы слотов, на которые ссылается `ChemReactorContainer`):

1. 2 входных предметных слота (`ItemStorageCoFH`, `StorageGroup.INPUT`) — индексы 0-1. Валидатор:
   `(item) -> this.filter.valid(item) && ChemReactorRecipeManager.instance().validItem(item)` —
   принимают только предметы, которые встречаются хоть в одном рецепте хоть в каком входном слоте
   (не позиционно).
2. 2 выходных предметных слота (`StorageGroup.OUTPUT`) — индексы 2-3. Валидатор `(item) -> false` —
   **обязательно явный**, т.к. `StorageGroup.OUTPUT` сам по себе НИЧЕГО не запрещает для ручной
   вставки через GUI-слот (это только для труб/капабилити-доступа); реальная защита — validator
   внутри `ItemStorageCoFH.isItemValid()`, дефолт которого `(e) -> true`. Машина сама пишет
   результат в эти слоты мимо validator'а, напрямую (`MachineBlockEntity.resolveOutputs()` вызывает
   `slot.setItemStack(...)` в обход `insertItem()`), так что это не мешает самой машине.
3. Charge-слот (`this.chargeSlot`, унаследован от `MachineBlockEntity`, `StorageGroup.INTERNAL`) —
   индекс 4. Зарядка энергией через предмет-батарейку; не участвует ни в позиционном поиске рецепта
   (не INPUT), ни в выдаче трубам (не OUTPUT).
4. 2 входных бака по 4000 mB (`FluidStorageCoFH`, `StorageGroup.INPUT`). Валидатор — **позиционный**:
   бак 0 → `ChemReactorRecipeManager.instance().validFluid(0, fluid)`, бак 1 → `validFluid(1, fluid)`.
   Раньше был один общий набор валидных жидкостей на оба бака — из-за этого при заливке через ведро
   или жидкостную ячейку одна и та же жидкость могла занять оба бака одновременно (спецификация:
   один бак = одна жидкость). Позиционность реализована как у `PressRecipeManager.validInputs`/
   `validDies` (те же раздельные `Set` для слота "предмет" и слота "die") — см. "Рецепты" ниже.
5. Выходной бак 8000 mB (`StorageGroup.OUTPUT`) — валидатор `(fluid) -> false`, по той же причине,
   что и выходные предметные слоты.
6. Аугмент-слоты — `this.addAugmentSlots(ThermalCoreConfig.machineAugments)` (дефолт Thermal — 4
   штуки). **Обязательно последними**: `AugmentableBlockEntity` считает аугменты последними
   `augSize()` слотами инвентаря (на этом основаны `onReplaced`/`createItemStackTag`/`load`).
7. `this.initHandlers()` — обязательный финальный шаг, строит `IItemHandler`/`IFluidHandler`.

Переопределённые методы:

- **`getBaseProcessTick()` → `40`.** ВАЖНАЯ ГРАБЛЯ: это НЕ количество тиков цикла, а **сколько FE
  тратится ЗА ОДИН ТИК** (см. `MachineBlockEntity.processTick()`: `energyStorage.modify(-this.
  processTick); this.process -= this.processTick`, где `this.processTick` изначально равен
  `this.baseProcessTick = getBaseProcessTick()`; также `getMaxSpeed()`/`getScaledSpeed()` трактуют
  его как скорость). Реальное время цикла = `ceil(энергия_рецепта / getBaseProcessTick())` тиков.
  Первая версия кода ошибочно возвращала `800` (перепутав с "800 тиков = 40 секунд" из
  спецификации) — при энергии рецепта 4000 FE это давало `ceil(4000/800)=5` тиков (0.25 сек) вместо
  40 секунд — цикл проходил почти мгновенно. Финальное калибровочное требование разработчика: время
  цикла должно совпадать с переплавкой руды в Electric Furnace самого Thermal (100 тиков = 5 сек;
  см. врезку "Electric Furnace — эталон тайминга" ниже), при сохранении энергии рецепта 4000 FE
  (вдвое больше, чем у печи). `4000 / 100 = 40` FE/тик — делится ровно, дробей не возникло.
- `cacheRecipe()` — ищет рецепт через `ChemReactorRecipeManager.instance().getRecipe(this)`,
  заполняет `itemInputCounts`/`fluidInputCounts`. `validateInputs()`/`validateOutputs()`/
  `processStart()`/`processFinish()` НЕ переопределяются — у `MachineBlockEntity` уже есть
  универсальная реализация по числу слотов/баков (как у `MachineBottlerTile`, у которой тоже выход
  только предметом).
- `createMenu(...)` — возвращает `new ChemReactorContainer(...)`. Сам вызов происходит из
  `TileBlockCoFH.use()` (родитель `ChemReactorBlock`) через `NetworkHooks.openScreen(...)`.

> **Electric Furnace — эталон тайминга (справочно).** `MachineFurnaceTile.getBaseProcessTick()`
> возвращает `FurnaceRecipeManager.instance().getBasePower()` — отдельное поле
> `AbstractManager.basePower`, по умолчанию **20** (никак не связано с `defaultEnergy`,
> настраивается отдельно через `setBasePower()`/конфиг). `FurnaceRecipeManager.DEFAULT_ENERGY =
> 2000`. Итого при стандартных настройках: 2000 FE / 20 FE-тик = 100 тиков = **5 секунд** на
> предмет. Это и есть текущий тайминг-эталон Chemical Reactor (см. выше).

### ChemReactorContainer

`extends cofh.core.inventory.container.TileCoFHContainer` (даёт бесплатно `quickMoveStack` —
шифт-клик, и `stillValid` — закрытие при разрушении блока/уходе игрока). Слоты (координаты — экранная
позиция рамки +1px, т.к. предмет рисуется на 1px внутрь рамки):

- Индексы 0-1 — входные предметные слоты: (38,53), (58,53).
- Индексы 2-3 — выходные предметные слоты: (119,22), (119,46).
- Индекс 4 — charge-слот: (8,53) — без отдельной GUI-рамки, использует уже нарисованную на фоне
  иконку (координаты подобраны разработчиком вручную по месту).
- Индексы 5..(5+`augSize()`-1) — аугмент-слоты, через штатный `this.bindAugmentSlots(tileInv, 5,
  this.tile.augSize())`. Координаты на экране НЕ задаются здесь — их полностью считает
  `AugmentPanel`/`ElementAugmentSlots` в `ChemReactorScreen` (выезжающая боковая панель).

### ChemReactorScreen

`extends cofh.core.client.gui.ContainerScreenCoFH<ChemReactorContainer>` — **не**
`cofh.thermal.lib.client.gui.MachineScreen` (тот требует кастомную PNG-панель по фиксированному
внутреннему формату Thermal, которой нет) и **не** голый vanilla `AbstractContainerScreen`
(наследовались от него до появления боковых панелей — пришлось сменить, см. ниже). Собственный фон/
предметные слоты/индикатор прогресса рисуются вручную через `blit()` в `renderBg()`;
`ContainerScreenCoFH.renderBg()` не вызывается (рисует свой `this.texture`, у нас другой подход) —
вместо этого в конце `renderBg()` вручную вызываются `drawPanels(false)`/`drawElements(false)`,
которые обычно вызывает родитель.

Конструктор задаёт `this.tile`, `this.name = "chemical_reactor"` и
`this.info = GuiHelper.generatePanelInfo("info.thermal_addon_chemreactor.chemical_reactor")`
(литеральный текст, не lang-key — `generatePanelInfo` сам читает ключи `.0`, `.1`, ... из
`lang/*.json`, пока ключ существует, и склеивает через `"\n\n"`; `ContainerScreenCoFH.init()`
сам добавляет `InfoPanel`, если `this.info` не пустой).

`init()` добавляет полный набор боковых панелей — то же, что даёт `AugmentableTileScreen.init()` +
`MachineScreen.init()` у родных машин Thermal (переписывать логику панелей не пришлось, только
собрать вызовы под свой тайл):

- `SecurityPanel(this, this.tile, SecurityHelper.getID(this.player))`;
- `AugmentPanel(this, this.menu::getNumAugmentSlots, this.menu.getAugmentSlots())` — выезжающая
  вкладка сбоку экрана, раскладка слотов (2×2/3×3/...) считается внутри `ElementAugmentSlots` по
  числу слотов, самим ничего не позиционировать. **Именно ради этой панели пришлось перейти с
  `AbstractContainerScreen` на `ContainerScreenCoFH`** — первая версия (статичная 2×2-сетка слотов
  прямо на холсте фона) была реализована раньше, проверена в игре и отклонена разработчиком как не
  соответствующая стилю Thermal;
- `RSControlPanel(this, this.tile)` — редстоун-управление;
- `ConfigPanel(this, this.tile, this.tile, this.tile::getFacing).addConditionals(ThermalGuiHelper.
  createDefaultMachineConfigs(this, this.name, this.tile))` — конфигурация вход/выход/оба/доступно
  по каждой из 6 граней. Иконки-превью граней внутри этой панели ищут текстуру
  `thermal:block/machines/machine_<this.name>` (`this.name="chemical_reactor"`) — такой текстуры
  нет (наша лицевая панель в СВОЁМ неймспейсе), поэтому иконки откатываются на общую
  `thermal:block/machines/machine_side`; некритично — эта панель отвечает только за конфигурацию
  сторон, а не за вид блока в мире (см. "Известные пробелы");
- `ThermalGuiHelper.createDefaultEnergyUserPanel(this, this.tile)` (панель "исп.энергии /
  макс.исп.энергии / эффективность") + элемент `GuiHelper.createDefaultEnergyStorage(this, 8, 8,
  this.tile.getEnergyStorage())` — вертикальная RF-полоска 16×42, позиция (8,8) — та же, что и у
  всех родных машин Thermal (`MachineScreen.init()`);
- 3 бака как настоящие `cofh.core.client.gui.element.ElementFluidStorage` (НЕ самодельная заливка
  через `blit()`+`fill()`, как было раньше): `GuiHelper.createSmallInputFluidStorage(...)` ×2
  (18×34, входные, позиции (37,14)/(57,14)) и `createMediumOutputFluidStorage(...)` (18×42,
  выходной, позиция (140,21)) — размеры один-в-один совпали с тем, что уже использовалось для рамок
  из `jei_handler.png`, переверстывать раскладку не пришлось. Даёт бесплатно: настоящую тайловую
  текстуру жидкости (`RenderHelper.drawFluid`, не плоский цвет), overlay-текстуру с делениями-
  рисочками, и тултип с локализованным названием жидкости и объёмом "X / Y mB".

`renderBg()` рисует (в таком порядке): фон `chemical_reactor.png` (256×256 холст, используется
176×166 в левом верхнем углу; на фоне уже нарисованы инвентарь игрока, хотбар и иконка charge-слота
— координаты иконки установлены пиксель-анализом PNG: примерно x9-20,y55-66, совпадает с позицией
charge-слота из `ChemReactorContainer`); рамки 4 предметных слотов (18×18, спрайт из
`thermal:textures/gui/jei_handler.png`, U/V=(0,96)); индикатор реакции (тот же файл, три кружка:
серый "выкл" всегда, синий "вкл" обрезается по ширине через `getScaledProgress(16)` и раскрывается
слева направо — решение автора аддона по умолчанию, не подтверждено разработчиком отдельно); затем
`drawPanels(false)`/`drawElements(false)` — всё вышеперечисленное из `init()`.

Зарегистрирован в `ChemReactorAddon` через `FMLClientSetupEvent` + `MenuScreens.register(...)` (НЕ
`RegisterMenuScreensEvent` — этот класс появился только в Forge для MC 1.20+, в 1.19.2 его нет).

## Рецепты

- `recipe/ChemReactorRecipe.java` — `extends cofh.thermal.lib.util.recipes.ThermalRecipe` (как
  `BrewerRecipe` у Thermal), только данные рецепта (вход/выход/энергия), без логики.
- `init/ChemReactorRecipeTypes.java` / `init/ChemReactorRecipeSerializers.java` — регистрация через
  готовые классы CoFH (`cofh.lib.util.recipes.SerializableRecipeType`,
  `cofh.thermal.lib.util.recipes.MachineRecipeSerializer`) — свой JSON-парсер писать не пришлось,
  `MachineRecipeSerializer` уже умеет читать смешанный список `"ingredients"` (предметы + жидкости
  вперемешку).
- `recipe/ChemReactorRecipeManager.java` — синглтон-менеджер (по образцу `BottlerRecipeManager`/
  `PressRecipeManager` из Thermal), ищет рецепт по содержимому 2 предметных слотов + 2 баков.
  **Поиск рецепта — строго позиционный** (как у `PressRecipeManager`, слот "die" = индекс 1): для
  текущего рецепта это значит `sulfur_dust` в предметном слоте 0 (слот 1 не используется), `latex` —
  в баке 0, `light_oil` — в баке 1 (порядок = порядок объявления в JSON `"ingredients"`). Переставить
  жидкости местами по бакам — рецепт не найдётся.
  - `validItem(ItemStack)` — НЕ позиционная (общий набор "встречается хоть в одном рецепте хоть в
    каком входном слоте предмета").
  - `validFluid(int tankIndex, FluidStack)` — **позиционная**: раздельные `Set<Fluid>`
    (`validFluidsTank0`/`validFluidsTank1`), заполняются из `addRecipe()` по индексу жидкости в
    `"ingredients"`. Нужна для того, чтобы один бак не принимал жидкость, предназначенную для
    другого (см. "ChemReactorTile" выше).
- Подключение к Thermal: `ThermalRecipeManagers.registerManager(ChemReactorRecipeManager.instance())`
  в конструкторе `ChemReactorAddon` — публичный API CoFH специально для аддонов, Thermal сам вызывает
  `refresh()` у всех зарегистрированных менеджеров при (пере)загрузке рецептов.
- **Грабли:** `RecipeManager.byType(RecipeType)` в official-маппингах **private** — внешний код
  вызвать его не может. Используем публичный `recipeManager.getAllRecipesFor(RecipeType)`
  (возвращает `List<T>` вместо `Map`), без access-transformer'а.
- JSON-рецепт: `data/thermal_addon_chemreactor/recipes/chem_reactor/dryrubber.json` — 500 mB
  `industrialforegoing:latex` + 500 mB `thermal:light_oil` + 1x `thermal:sulfur_dust` →
  1x `industrialforegoing:dryrubber`, `"energy": 4000`.

## Аугменты

Полная поддержка "из коробки" через `MachineBlockEntity`/`AugmentableBlockEntity`. Предметы-
аугменты физически лежат в `thermal_core` (НЕ в отдельном моде Thermal Innovation — было ошибочное
предположение в начале работы над этим, не подтвердилось при распаковке jar'а). Пример
registry-имени для теста: `thermal:machine_speed_augment` ("Flux Linkage Amplifier") — можно выдать
через `/give <ник> thermal:machine_speed_augment`, не обязательно крафтить (крафт-рецепт закрыт
условием `"thermal:flag" "machine_augments"`, но на `/give` это не влияет). Полный список — файлы
`data/thermal/recipes/augments/*.json` внутри `thermal_core-1.19.2-10.3.0.9.jar`.

## Ресурсы и текстуры

- **Модель блока:** `models/block/chemical_reactor.json` — `"parent": "thermal:block/machine_base"`
  (НЕ `minecraft:block/orientable`, как было раньше), переопределяет только текстуру `"north"`
  (верх/низ/бока/зад — общие текстуры Thermal `machine_top`/`machine_side`/`machine_bottom`, заданы
  в самом `machine_base.json`). `models/block/chemical_reactor_active.json` — `"parent"` на
  статичную модель, `"north"` заменена на активную текстуру. `machine_base.json` несёт
  `"loader": "thermal:reconfigurable"` (наследуется дочерними моделями через `"parent"`) —
  это кастомный `IDynamicBakedModel`
  (`cofh.thermal.core.client.renderer.model.ReconfigurableBakedModel`), который рисует поверх
  каждой грани блока цветную иконку входа/выхода/обоих/недоступно в зависимости от конфигурации
  сторон (та же система, что рисует иконки-превью в `ConfigPanel`, но применённая к реальному блоку
  в мире). Blockstate (`blockstates/chemical_reactor.json`) — 8 вариантов (`facing` × `active`);
  раньше свойства `active` в blockstate не было вообще, хотя текстура `chemical_reactor_active.png`
  уже существовала на диске — не была подключена.
- Лицевая панель (front, статика): `chemical_reactor.png`.
- Лицевая панель (активное состояние): `chemical_reactor_active.png` — спрайт 6 кадров по
  вертикали + `.mcmeta` с анимацией (интерполяция включена, `frametime: 8`).
- Lang-файлы: `en_us`, `ru_ru` — название блока + ключи
  `info.thermal_addon_chemreactor.chemical_reactor.0` (и далее `.1`, `.2`... если понадобится) для
  `InfoPanel` в GUI.
- **Текстура GUI-панели:**
  `src/main/resources/assets/thermal_addon_chemreactor/textures/gui/container/chemical_reactor.png`
  — холст 256×256, используется область 176×166 в левом верхнем углу. На фоне нарисованы инвентарь
  игрока (9×3 + хотбар) и иконка charge-слота (координаты по пиксель-анализу — см. раздел GUI).
  Поверх неё код накладывает через `blit()` только рамки 4 предметных слотов и индикатор реакции
  (`thermal:textures/gui/jei_handler.png`, общий ассет Thermal внутри `thermal_core`, свою копию
  класть не нужно). Баки, RF-полоска и все боковые панели — не свои спрайты, а стандартные
  текстуры/элементы `cofh_core`, подключённые через `GuiHelper`/`ThermalGuiHelper` в `init()`, свои
  ассеты под них заводить не пришлось.

## Спецификация механизма "Chemical Reactor"

**Входы:**
- 2 бака для жидкостей, по 4000 mB каждый (позиционно закреплены за конкретной жидкостью рецепта)
- 2 слота для предметов (слот 0 — используется, слот 1 — зарезервирован под будущие рецепты)

**Выходы:**
- 1 бак для жидкости, 8000 mB
- 2 слота для предметов

**Обработка:**
- Время цикла: 100 тиков = 5 секунд (калибровано под Electric Furnace Thermal — см. раздел
  "ChemReactorTile"/`getBaseProcessTick()`)
- Потребление энергии: 4000 FE за полный цикл (40 FE/тик при базовой скорости)
- Полная поддержка аугментов Thermal "из коробки"

**Тестовый рецепт (проверен в игре, работает):**
- Вход: 500 mB `industrialforegoing:latex` + 500 mB `thermal:light_oil` + 1x `thermal:sulfur_dust`
- Выход: 1x `industrialforegoing:dryrubber`

(500 mB — объём для теста рецепта; ёмкость баков 4000/4000/8000 mB — отдельный параметр, ёмкость ≠
объём в конкретном рецепте.)

## Известные пробелы (сознательно отложено, не баг)

- **Текстуры GUI — не финальный уникальный арт.** Собственный фон (`chemical_reactor.png`) +
  позаимствованные спрайты Thermal из `jei_handler.png` (рамки предметных слотов, индикатор
  реакции) + стандартные текстуры/элементы `cofh_core` (баки, RF-полоска, панели). Когда появится
  финальный арт под Chemical Reactor — заменить константы координат/файл текстуры в
  `ChemReactorScreen`, сам код `blit()`/элементы менять не придётся.
- **Иконки-превью граней в `ConfigPanel`** откатываются на общую
  `thermal:block/machines/machine_side` (наша лицевая панель в своём неймспейсе, не
  `thermal:block/machines/machine_chemical_reactor`) — некритично, починится вместе с финальным
  артом, если понадобится точность.
- В мире (не в GUI) визуально **не подтверждено**, что цветные иконки входа/выхода реально
  появляются на гранях блока при переключении конфигурации сторон в `ConfigPanel`
  (`ReconfigurableBakedModel`, см. раздел "Ресурсы и текстуры") — единственное место в проекте, где
  пришлось не переиспользовать готовый код CoFH один-в-один, а собирать model/blockstate JSON
  самостоятельно по образцу, так что здесь выше шанс расхождения с ожиданием.

## Текущий статус

**Всё готово и проверено в игре (подтверждено разработчиком):**
- Окружение, зависимости, регистрация блока/тайла/рецептов/GUI — см. соответствующие разделы выше.
- Блок ставится, поворачивается к игроку, тикает, открывает GUI по ПКМ.
- Полный набор боковых панелей в GUI (Security/Augment/RSControl/Config/Energy/Info) — открываются,
  работают.
- Аугмент-слот принимает предмет-аугмент.
- Полный цикл обработки тестового рецепта (dryrubber) — проходит от начала до конца.
- Заливка/слив жидкости через ведро и жидкостную ячейку — позиционно, один бак = одна жидкость.
- Выходные слоты/бак не принимают ручную вставку игроком.
- Charge-слот — на своём месте, без лишней рамки.
- Время цикла и расход энергии — откалиброваны и подтверждены.

**Не проверено / осталось сделать** — см. "Известные пробелы" выше и "План разработки" ниже.

## Связанные, но отдельные задачи (НЕ трогать в рамках этого аддона)

В модпаке уже используется KubeJS + аддон kubejs-thermal для модификации рецептов существующих
машин Thermal (переименование Brewer, замена жидкости latex). Эта работа уже завершена и не
относится к текущему Java-аддону — не путать и не пересекать с этой задачей.

## План разработки

1. ✅ Настройка проекта — build.gradle, mods.toml, зависимости в libs/
2. ✅ Регистрация блока и предмета
3. ✅ BlockEntity (Tile) — проверено в игре
4. ✅ Система рецептов — тип `chem_reactor`, сериализатор, менеджер, тестовый рецепт dryrubber,
   проверен полный цикл в игре
5. ✅ Container — слоты, баки, charge-слот, аугмент-слоты
6. ✅ Screen (GUI) — открывается в игре, полный набор боковых панелей, реальные элементы баков/энергии
7. 🟡 Assets — blockstate/модели/lang готовы; GUI-текстуры подключены, но это **не финальный арт**
   (см. "Известные пробелы")
8. ⬜ Сборка и тест — Gradle build, jar в модпак, проверка в игре на реальном сервере

**Дальше имеет смысл (не пронумеровано строго, на усмотрение):**
- Проверить в мире (не в GUI), появляются ли цветные иконки конфигурации на гранях блока при смене
  настроек в `ConfigPanel` (см. "Известные пробелы") — единственный участок с самостоятельно
  собранным JSON, не проверенный визуально.
- Финальный уникальный арт GUI (фон/баки/прогресс/энергия) взамен текущих позаимствованных
  спрайтов — когда появится, менять только координаты/файл текстуры, код рендера трогать не нужно.
- Второй и последующие рецепты для реактора — при добавлении держать в голове позиционность
  (слот 0 предмета, баки 0/1 жидкостей — фиксированные позиции, см. "Рецепты").
- (После 8) — упаковать jar и закинуть в реальный модпак на сервере (Forge 43.3.5).
