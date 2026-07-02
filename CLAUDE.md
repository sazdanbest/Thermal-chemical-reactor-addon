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
- `jei-1.19.2-forge-11.8.1.1034.jar`, `kubejs-forge-1902.6.2-build.73.jar`,
  `rhino-forge-1902.2.3-build.284.jar`, `architectury-6.6.92-forge.jar` — для мягкой (опциональной)
  интеграции с JEI/KubeJS, см. разделы "JEI-интеграция"/"KubeJS-интеграция" ниже.
  `kubejs-thermal-1902.1.9-build.16.jar` тоже лежит в `libs/`, но **не** подключён в build.gradle —
  используется только как справочный материал (уже готовый пример recipe schema для родных машин
  Thermal), у нас своя аналогичная схема, а не зависимость от него.

**Важная особенность build.gradle:** в ForgeGradle 5 через `flatDir` координаты deobf-зависимости
обязательно должны быть явно разделены на группу/имя/версию. Пустая группа ломает резолвер. Если имя
файла содержит classifier (например `architectury-6.6.92-forge.jar` = artifactId `architectury`,
версия `6.6.92`, classifier `forge`), координата пишется как `group:name:version:classifier`.

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
    implementation fg.deobf("jei:jei-1.19.2-forge:11.8.1.1034")
    implementation fg.deobf("kubejs:kubejs-forge:1902.6.2-build.73")
    implementation fg.deobf("rhino:rhino-forge:1902.2.3-build.284")
    implementation fg.deobf("architectury:architectury:6.6.92:forge")
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

**Грабли: KubeJS не грузится в `runClient` этого проекта (Mixin refmap), и это не чинится.**
Начиная с этой сессии в проект добавлены `jei`/`kubejs`/`rhino`/`architectury` (см. "Зависимости"
выше) для мягкой интеграции. JEI в dev-окружении грузится нормально (в логе `runClient` по нему нет
ни одной ошибки). А вот `kubejs-forge` **гарантированно падает** при попытке `runClient` в этом
проекте с крашем ещё на этапе загрузки классов (до того, как Forge вообще успевает дойти до
конструктора нашего мода):

```
Critical injection failure: @Inject annotation on shutdownExecutorsKJS could not find any targets
matching 'Lnet/minecraft/Util;m_137580_()V' in net.minecraft.Util. Using refmap
kubejs-common-refmap.json ...
```

**Причина — подтверждена побайтовым сравнением, не гипотеза:** `kubejs-common-refmap.json` внутри
исходного `libs/kubejs-forge-*.jar` и внутри деобфусцированного jar'а из
`~/.gradle/caches/forge_gradle/deobf_dependencies/kubejs/...` — **идентичны байт-в-байт** (одинаковый
размер, одинаковое содержимое). Значит `fg.deobf` remap'ит только SRG-имена **внутри .class-байткода**
и не трогает произвольные JSON-ресурсы вроде Mixin refmap — а в этом refmap'е записаны сырые SRG-имена
(`m_137580_` и т.п.), которые Mixin должен на лету перевести в official-имена, чтобы найти метод в
уже official-маппированном классе `Util`. В боевом (продакшен) Forge за это отвечает отдельный,
более полный сервис ремаппинга миксинов, который есть у полноценной установленной игры/сервера, но
которого нет (или он не работает так же для "чужих" зависимостей) в ForgeGradle userdev
dev-окружении (`runClient`) стороннего мода вроде нашего.

**Проверено и НЕ помогает:** добавление `architectury` (обязательная зависимость самого KubeJS,
без него Forge пишет `Missing or unsupported mandatory dependencies`) устраняет только эту
ошибку про зависимость, но не Mixin-краш — он идентичен что до, что после. Смена версии
`kubejs-forge` тоже не рассматривается как решение — **разработчик подтвердил, что именно эти версии
файлов (`kubejs-forge-1902.6.2-build.73`, `rhino-forge-1902.2.3-build.284`,
`architectury-6.6.92-forge`, `kubejs-thermal-1902.1.9-build.16`) уже штатно работают вместе на
реальном сервере модпака** — значит дело не в версиях, а именно в ограничении локального dev-окружения.

**Практический вывод:**
- KubeJS-интеграцию (см. раздел "KubeJS-интеграция" ниже) **нельзя визуально протестировать через
  `runClient` в этом проекте** в текущем виде. Компилируется она нормально (`compileJava` проходит),
  но живьём в игре внутри этого dev-окружения не запускается.
- Единственный способ увидеть её в работе — собрать jar (`./gradlew build`) и поставить в реальный
  модпак на сервере, где KubeJS уже штатно установлен вместе с Architectury.
- Если всё же нужно локально проверить в игре ОСТАЛЬНОЕ (блок/GUI/JEI/рецепты) без KubeJS — временно
  убрать три строки `fg.deobf("kubejs:...")`/`fg.deobf("rhino:...")`/`fg.deobf("architectury:...")`
  из `build.gradle`, прогнать `runClient`, потом вернуть обратно. Полноценно так и не делали в рамках
  этой сессии (разработчик попросил вместо этого сразу зафиксировать находки и закоммитить).
- Если в новой сессии опять возникнет соблазн "починить" эту Mixin-ошибку — не тратить на это время
  заново, находка уже задокументирована здесь.

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
- Мягкие (опциональные, `mandatory = false`) зависимости: `jei`, `kubejs` — мод спокойно грузится и
  работает без них, интеграция включается только если они установлены. См. "JEI-интеграция"/
  "KubeJS-интеграция" ниже.

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
- **`resolveInputs()` — переопределён (фикс серверного краша).** Родительская реализация
  (`MachineBlockEntity.resolveInputs()`) применяет i-й счётчик из `itemInputCounts`/
  `fluidInputCounts` к i-му слоту/баку **безусловно, даже нулевой**. Для предметов `consume(0)` на
  пустом слоте безобиден, а вот `FluidStorageCoFH.modify(0)` на пустом баке падает:
  `modify()` безусловно вызывает `this.fluid.setAmount(...)`, а `this.fluid` пустого бака — это
  синглтон `FluidStack.EMPTY`, у которого `setAmount()` бросает
  `IllegalStateException("Can't modify the empty stack.")` → `ReportedException: Ticking block
  entity` → краш сервера ровно в момент завершения цикла. Воспроизводилось на реальном сервере с
  KubeJS-рецептом "1 жидкость + 2 предмета" (незанятый бак 1 получал счётчик `0`). Наша версия
  пропускает нулевые счётчики (и на всякий случай ограничивает индексы размером списков) — нули для
  незадействованных входов теперь штатная ситуация, т.к. счётчики выровнены по всем слотам/бакам
  машины (см. `ChemReactorMachineRecipe` в "Рецепты").
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
- `ConfigPanel(this, this.tile, this.tile, this.tile::getFacing).addConditionals(...)` —
  конфигурация вход/выход/оба/доступно по каждой из 6 граней. 5 из 6 квадратиков-превью —
  штатные `ThermalGuiHelper.createDefaultMachineConfigTop/Left/Right/Bottom/Back(...)` (честно
  откатываются на общие текстуры `machine_top`/`machine_side`/`machine_bottom` — блок их и в мире
  использует через `thermal:block/machine_base`). **Центральный квадратик (лицевая грань) — свой
  метод `createMachineConfigFace()`**: штатный `createDefaultMachineConfigFace()` ищет спрайт
  строго в неймспейсе Thermal (`"thermal:block/machines/machine_" + name`), нашей текстуры там нет,
  и раньше центр молча откатывался на безликую `machine_side` (жалоба разработчика). Свой метод —
  побайтовая копия хелпера, но со спрайтами `thermal_addon_chemreactor:block/chemical_reactor`
  (выкл) / `chemical_reactor_active` (вкл; спрайт анимированный — в панели тоже анимируется) — оба
  уже в атласе блоков, т.к. на них ссылается модель блока. Слои как у хелпера: жидкость процесса
  (`getRenderFluid()`, если не пуста) под лицевой текстурой; порядок массива в `addConditionals`
  фиксированный: top, left, face, right, bottom, back;
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

Координаты/спрайты (`TEXTURE_BACKGROUND`, `TEXTURE_WIDGETS`, `SLOT_*`, `TANK_*`, `PROGRESS_*`,
`SPRITE_*`) объявлены как `public static final` (не `private`) — специально, чтобы
`ChemReactorRecipeCategory` (см. "JEI-интеграция") мог переиспользовать те же числа и тот же файл
спрайтов вместо того, чтобы дублировать магические координаты в другом пакете.

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
  **Правила совпадения (переработаны после серверного краша на KubeJS-рецепте, см.
  `resolveInputs()` в "ChemReactorTile"):**
  - **Жидкости — строго позиционные:** первая жидкость в JSON `"ingredients"` обязана быть в баке
    0, вторая — в баке 1 (как у `PressRecipeManager`, слот "die" = индекс 1). Переставить жидкости
    местами по бакам — рецепт не найдётся.
  - **Предметы — НЕ позиционные** (просьба разработчика): порядок раскладывания реагентов по двум
    входным слотам не важен, ключ поиска сортирует предметы канонически (по `hashCode`
    `ComparableItemStack` — рецептная и инвентарная стороны сортируются согласованно).
  - **Любой вход может быть пустым:** рецепт может занимать 0-2 предмета и 0-2 жидкости (но хотя бы
    что-то одно). Пустые заглушки `ItemStack.EMPTY`/`FluidStack.EMPTY` **вычищаются до** создания
    внутреннего рецепта и не попадают в его списки — раньше рецепт с одной жидкостью хранил
    `[latex, FluidStack.EMPTY]`, из-за чего `getInputFluidCounts()` отдавал счётчик и для пустого
    бака, а `MachineBlockEntity.resolveInputs()` ронял сервер (см. "ChemReactorTile").
    Незадействованные рецептом слоты/баки должны быть пустыми в машине — иначе содержимое не
    совпадает с ключом и цикл не начнётся (то же поведение, что у родных машин Thermal).
  - **Ключ поиска** = отсортированные предметы + ровно 2 "ячейки" жидкостей (хэш жидкости бака или
    `0`, если бак пуст) — по ячейке на КАЖДЫЙ бак, чтобы жидкость из бака 1 не могла "съехать" на
    позицию бака 0 в ключе (раньше пустые баки просто пропускались).
  - **Внутренний рецепт — `ChemReactorMachineRecipe extends SimpleMachineRecipe`** (вложенный класс
    менеджера): счётчики потребления выровнены по реальным слотам/бакам инвентаря, а не по списку
    ингредиентов рецепта. Предметные — сопоставлением по содержимому с флагами `used[]` (как
    `BaseMachineRecipe.getInputItemCounts()`, но без "прилипания" двух одинаковых требований к
    одному слоту), жидкостные — напрямую по индексам (ингредиент k = бак k). Незадействованные
    входы получают счётчик `0` — их пропускает наш `resolveInputs()`.
  - **Валидация лимитов:** рецепты с >2 входных предметов, >2 входных жидкостей, >2 выходных
    предметов или >1 выходной жидкости отклоняются **целиком** с warn-логом (`LogUtils`), как и
    рецепты вовсе без входов — раньше лишние ингредиенты молча отбрасывались (KubeJS-рецепт с
    опечаткой выглядел бы "работающим", но потреблял не всё).
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
  `industrialforegoing:latex` + 100 mB `thermal:light_oil` + 1x `thermal:sulfur_dust` →
  1x `industrialforegoing:dryrubber` + 100 mB `industrialforegoing:sludge`, `"energy": 4000`.
  (`light_oil` уменьшен с 500 до 100 mB, и добавлен выходной `sludge` в выходной бак — по просьбе
  разработчика после теста в игре; выходной предметный слот `dryrubber` не менялся.)

## JEI-интеграция

Мягкая (опциональная) — мод и без установленного JEI работает как обычно, интеграция включается
сама, только если JEI есть в модпаке. Код — `client/compat/jei/`, две части:

- `ChemReactorJeiPlugin implements IModPlugin`, аннотирован `@JeiPlugin`. Регистрирует один
  `RecipeType<ChemReactorRecipe>` (`new RecipeType<>(ChemReactorRecipeTypes.CHEM_REACTOR.getId(),
  ChemReactorRecipe.class)`), одну категорию, сам блок реактора как "катализатор" (иконка "сделано
  в..." в JEI), и `registerGuiHandlers()` с `addRecipeClickArea(...)` — клик по индикатору реакции на
  настоящем экране (`ChemReactorScreen.PROGRESS_X/Y/SPRITE_PROGRESS_W/H`) открывает список рецептов
  в JEI, как у всех родных машин Thermal.
- `ChemReactorRecipeCategory extends cofh.thermal.lib.compat.jei.ThermalRecipeCategory<ChemReactorRecipe>`
  — родитель (часть `thermal_core`, уже обязательная зависимость аддона) даёт бесплатно RF-иконку с
  тултипом. Фон категории — **пустая канва** (`guiHelper.createBlankDrawable(...)`), а не кроп
  настоящего `chemical_reactor.png`: у родных машин Thermal фон категории — кроп их собственного
  `MachineXScreen.TEXTURE`, потому что их арт-стиль рисует рамки слотов прямо на этой текстуре; наш
  `chemical_reactor.png` так не делает (рамки/баки рисуются отдельно поверх через `blit()`/
  `ElementFluidStorage`, см. "ChemReactorScreen" выше) — кроп дал бы пустые проплешины на месте
  баков/слотов. Рамки предметных слотов и спрайт индикатора реакции — общие спрайты Thermal
  (`cofh.thermal.lib.compat.jei.Drawables`, файл `thermal:textures/gui/jei_handler.png` — тот же, что
  и `ChemReactorScreen.TEXTURE_WIDGETS`; индикатор — те же `SPRITE_PROGRESS_*` константы из
  `ChemReactorScreen`) — для визуальной согласованности между GUI и JEI-страницей. Рамки **баков**
  — НЕ из `Drawables` (там общий ассет 18×62 у всех трёх размеров сразу — слишком высокий, чтобы под
  баком поместился ещё и предметный слот), а те же файлы, что реально рисует `ChemReactorScreen` через
  `GuiHelper.createSmallInputFluidStorage()`/`createMediumOutputFluidStorage()`:
  `cofh_core:textures/gui/elements/storage_fluid_small.png` (18×34, входные баки) и
  `storage_fluid_medium.png` (18×42, выходной бак) — оба физически 32×64 на диске, поэтому
  подключены через `guiHelper.drawableBuilder(...).setTextureSize(32, 64).build()`, а не через
  упрощённый `createDrawable(...)` (тот по умолчанию считает текстуру 256×256 и дал бы неверный UV).
  Раскладка повторяет основной GUI буквально: бак сверху, свой предметный слот строго под ним, для
  обоих входов; выход остался колонкой (2 предметных слота) + отдельный бак справа.

**Как разрабатывалось — важно для будущих правок.** Ни одна деталь API JEI/Thermal-JEI-хелперов не
угадывалась — каждый класс/метод (`IModPlugin`, `IRecipeCategory`, `IRecipeLayoutBuilder.addSlot`,
`ForgeTypes.FLUID_STACK`, `ThermalRecipeCategory`, `Drawables`, `TCoreJeiPlugin.tankSize()`/
`defaultFluidTooltip()`/`defaultOutputTooltip()`) сверялся через декомпиляцию реальных классов
Thermal (`cofh.thermal.expansion.compat.jei.machine.BrewerRecipeCategory`/`PressRecipeCategory`,
`cofh.thermal.expansion.compat.jei.TExpJeiPlugin`, `cofh.thermal.core.compat.jei.TCoreJeiPlugin`,
`cofh.thermal.lib.compat.jei.{ThermalRecipeCategory,Drawables}`) через ForgeFlower (см. "Декомпиляция
Thermal/CoFH для справки" выше) и/или `javap -p` на классах из самого `jei-1.19.2-forge-*.jar`. Если
понадобится второй рецепт/другая раскладка слотов — начинать стоит оттуда же, а не гадать сигнатуры.

**Обфускация:** `jei-1.19.2-forge-*.jar`, как и остальные зависимости в `libs/`, содержит сырые
SRG-идентификаторы в байткоде (проверено грепом по `.class`-файлам на паттерн `[mf]_\d+_`) — значит
тоже идёт через `fg.deobf`, а не голым classpath-jar'ом.

**В деле в игре — подтверждено разработчиком на реальном сервере модпака** (там KubeJS уже штатно
установлен и работает — Mixin-краш из "Грабли: KubeJS не грузится в runClient" оказался ограничением
именно локального `runClient`-окружения, как и предполагалось, в проде не воспроизводится). После
первого визуального теста разработчик попросил поправить раскладку: баки на переехали над своими
предметными слотами (вместо бок о бок), выходной бак стал `TANK_MEDIUM`-размера (см. выше), и вся
входная группа (2 бака + 2 предметных слота) сдвинута на +22px по X — версия мода после этих правок:
**1.0.2**. Локальный `runClient` в этом проекте по-прежнему падает на KubeJS (см. жирный блок в
начале файла) — это не мешает проверке на реальном сервере и не является багом самого JEI/аддона.

## KubeJS-интеграция

Мягкая (опциональная) — так же, как и JEI. Даёт возможность добавлять/убирать рецепты Chemical
Reactor через KubeJS-скрипты модпака. Код — `integration/kubejs/ChemReactorKubeJSPlugin.java`
(`extends dev.latvian.mods.kubejs.KubeJSPlugin`), плюс `src/main/resources/kubejs.plugins.txt` —
единственная точка, через которую KubeJS вообще узнаёт о существовании этого класса (см. "Как это
обнаруживается" ниже).

Регистрирует recipe schema из трёх полей:

```java
RecipeKey<Either<InputFluid, InputItem>[]> INGREDIENTS =
        FluidComponents.INPUT_OR_ITEM_ARRAY.key("ingredients").alt("ingredient");
RecipeKey<Either<OutputFluid, OutputItem>[]> RESULTS =
        FluidComponents.OUTPUT_OR_ITEM_ARRAY.key("results").alt("result");
RecipeKey<Integer> ENERGY = NumberComponent.INT.key("energy");

RecipeSchema SCHEMA = new RecipeSchema(INGREDIENTS, RESULTS, ENERGY);
```

Имена JSON-полей (`"ingredients"`/`"result"`/`"energy"`) выбраны **намеренно совпадающими** с тем,
что уже читает наш `ChemReactorRecipeSerializers.CHEM_REACTOR_SERIALIZER`
(`cofh.thermal.lib.util.recipes.MachineRecipeSerializer`, общий формат Thermal) — значит JSON,
который сгенерирует KubeJS по этой схеме, читается уже существующим
`ChemReactorRecipe`/`ChemReactorRecipeManager` без единой правки. Те же имена полей использует и
`BasicRecipeSchema` внутри `kubejs-thermal-1902.1.9-build.16.jar` (уже используемого в модпаке для
родных машин Thermal — Brewer, Press и т.п.) — совпадение не случайное, а следствие того, что мы и
так следовали общему формату рецептов Thermal с самого начала (см. "Рецепты" выше).

**Как это обнаруживается (обязательно для понимания, почему это безопасно без KubeJS):** KubeJS сам
(класс `dev.latvian.mods.kubejs.util.KubeJSPlugins`) при старте сканирует **все** загруженные моды в
поисках ресурса `kubejs.plugins.txt` и, если находит, рефлексивно создаёт перечисленные там классы —
но только если сам KubeJS установлен и это делает. Наш собственный код (`ChemReactorAddon` и другие)
никогда не импортирует и не создаёт `ChemReactorKubeJSPlugin` напрямую — если KubeJS не установлен,
никто этот файл не читает и класс, ссылающийся на `dev.latvian.mods.kubejs.*`, никогда не
загружается JVM → никакого `NoClassDefFoundError`. Ровно та же схема, что и у JEI (`@JeiPlugin`
сканируется самим JEI через ASM, без реальной загрузки класса, если JEI нет).

**В деле в игре — не подтверждено и не может быть подтверждено локально** — см. отдельный жирный
блок "Грабли: KubeJS не грузится в runClient" в начале файла. Коротко: сам `kubejs-forge` (не наш
код) падает с Mixin-ошибкой в этом dev-окружении, поэтому визуально проверить
`event.recipes.thermal_addon_chemreactor.chem_reactor(...)` в этом проекте через `runClient` не
получится — нужно тестировать на реальном сервере модпака, где KubeJS уже штатно работает.

## Как правильно взаимодействовать с этим аддоном через KubeJS

Для авторов модпака/скриптов — как добавлять, менять и убирать рецепты Chemical Reactor из KubeJS
(`kubejs/server_scripts/*.js`), без правки Java-кода этого аддона. Актуально для версии мода
**1.0.3+** (в 1.0.2 и раньше рецепт с незанятым баком ронял сервер при завершении цикла — см.
`resolveInputs()` в "ChemReactorTile").

**Слоты машины (что физически доступно рецепту):**

| Вход | Выход |
|---|---|
| до 2 предметов (порядок НЕ важен) | до 2 предметов |
| до 2 жидкостей (позиционно: 1-я → бак 0, 2-я → бак 1) | до 1 жидкости |

Любую часть можно не использовать (мин. 1 любой ингредиент на входе и 1 любой результат на выходе).
Рецепт с превышением лимитов отклоняется целиком с warn-сообщением в лог сервера — если рецепт
"не появился", первым делом искать в логе `Chemical Reactor recipe ... rejected`.

**Добавить новый рецепт (обе формы записи равнозначны):**

```js
ServerEvents.recipes(event => {
    // Форма 1 — короткая (Fluid.of/Item.of):
    event.recipes.thermal_addon_chemreactor.chem_reactor([
        Fluid.of('industrialforegoing:latex', 500), // 1-я жидкость списка → бак 0 (позиционно!)
        Fluid.of('thermal:light_oil', 100),         // 2-я жидкость списка → бак 1 (позиционно!)
        Item.of('thermal:sulfur_dust')              // предметы — в любой входной слот, порядок не важен
    ], [
        Item.of('industrialforegoing:dryrubber'),   // выходной предметный слот
        Fluid.of('industrialforegoing:sludge', 100) // выходной бак
    ]).energy(4000)

    // Форма 2 — объектная (то, что KubeJS сам разворачивает в тот же JSON):
    event.recipes.thermal_addon_chemreactor.chem_reactor({
        ingredients: [
            { fluid: 'industrialforegoing:latex', amount: 500 },
            { item: 'thermal:sulfur_dust' },
            { item: 'thermal:tar' }
        ],
        result: [
            { item: 'ic2:rubber' }
        ],
        energy: 4000
    })
})
```

- `ingredients` — предметы и жидкости можно перемешивать в любом порядке записи. Позиция важна
  **только для жидкостей относительно друг друга**: первая жидкость списка → бак 0, вторая → бак 1
  (переставить их местами по бакам в машине — рецепт не соберётся). Предметы игрок/трубы могут
  класть в любой из двух входных слотов в любом порядке.
- Количество предметов — `Item.of('mod:item', 3)` или `{ item: 'mod:item', count: 3 }`; объём
  жидкости — `Fluid.of('mod:fluid', 250)` или `{ fluid: 'mod:fluid', amount: 250 }` (в mB; больше
  4000 mB на входной бак / 8000 mB на выходной физически не поместится — рецепт никогда не
  запустится).
- `result`/`results` — выходные предметы и/или жидкости, тоже вперемешку. Метод `.energy(...)`
  навешивается отдельно (форма 1), т.к. `energy` — не часть аргументов конструктора, а отдельное
  поле схемы; если не указать — по умолчанию 4000 FE (`DEFAULT_ENERGY`). Время цикла =
  `energy / 40` тиков при базовой скорости (4000 FE = 100 тиков = 5 сек).
- Разрешены и alt-имена `ingredient`/`result` (без `s`) — совместимо с тем, как то же самое иногда
  пишут для родных машин Thermal через `kubejs-thermal`.
- Незадействованные рецептом слоты/баки должны быть **пустыми** в машине в момент старта цикла —
  посторонний предмет/жидкость в незанятом входе не даст рецепту совпасть (то же поведение, что у
  родных машин Thermal).

**Убрать/заменить существующий рецепт (например тестовый `dryrubber`):**

```js
ServerEvents.recipes(event => {
    event.remove({ id: 'thermal_addon_chemreactor:chem_reactor/dryrubber' })
    // ... и добавить свой через event.recipes.thermal_addon_chemreactor.chem_reactor(...)
    // Убрать все рецепты реактора разом: event.remove({ type: 'thermal_addon_chemreactor:chem_reactor' })
})
```

**Не через KubeJS-схему (не относится к этому аддону), но нужно для контекста:** переименование
Brewer и замена жидкости latex через `kubejs-thermal` — это уже сделанная, отдельная работа над
самими машинами Thermal (см. "Связанные, но отдельные задачи" выше), она не трогает и не пересекает
рецепты Chemical Reactor.

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
  `InfoPanel` в GUI. Ключ `itemGroup.thermal_addon_chemreactor` **убран** — был лишним, никакой
  `CreativeModeTab` в коде так и не завели (`ChemReactorItems` кладёт предмет в ванильный
  `CreativeModeTab.TAB_MISC`), ключ ни на что не влиял.
- **Удалены неиспользуемые файлы:** `textures/block/chemical_reactor_top.png`, `..._side.png`,
  `..._bottom.png` — остались от старой модели (до перехода `models/block/chemical_reactor.json` на
  `"parent": "thermal:block/machine_base"`, который сам берёт верх/бок/низ из общих текстур Thermal,
  см. выше). Ничего в коде/JSON на них уже не ссылалось.
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
- 2 слота для предметов (НЕ позиционные с 1.0.3 — реагенты можно класть в любом порядке; рецепт
  может использовать 0, 1 или 2 предмета)

**Выходы:**
- 1 бак для жидкости, 8000 mB
- 2 слота для предметов

**Обработка:**
- Время цикла: 100 тиков = 5 секунд (калибровано под Electric Furnace Thermal — см. раздел
  "ChemReactorTile"/`getBaseProcessTick()`)
- Потребление энергии: 4000 FE за полный цикл (40 FE/тик при базовой скорости)
- Полная поддержка аугментов Thermal "из коробки"

**Тестовый рецепт (проверен в игре, работает):**
- Вход: 500 mB `industrialforegoing:latex` + 100 mB `thermal:light_oil` + 1x `thermal:sulfur_dust`
- Выход: 1x `industrialforegoing:dryrubber` + 100 mB `industrialforegoing:sludge`

(500/100 mB — объём для теста рецепта; ёмкость баков 4000/4000/8000 mB — отдельный параметр, ёмкость
≠ объём в конкретном рецепте. `light_oil` уменьшен с 500 до 100 mB и добавлен выходной `sludge` —
правка по результатам теста в игре, версия мода 1.0.2.)

## Известные пробелы (сознательно отложено, не баг)

- **Текстуры GUI — не финальный уникальный арт.** Собственный фон (`chemical_reactor.png`) +
  позаимствованные спрайты Thermal из `jei_handler.png` (рамки предметных слотов, индикатор
  реакции) + стандартные текстуры/элементы `cofh_core` (баки, RF-полоска, панели). Когда появится
  финальный арт под Chemical Reactor — заменить константы координат/файл текстуры в
  `ChemReactorScreen`, сам код `blit()`/элементы менять не придётся.
- ~~**Иконки-превью граней в `ConfigPanel`** откатываются на общую `machine_side`~~ — **закрыто в
  1.0.3**: центральный квадратик (лицевая грань) теперь рисует нашу текстуру
  `thermal_addon_chemreactor:block/chemical_reactor`(`_active`) через свой
  `createMachineConfigFace()` в `ChemReactorScreen` (см. раздел "ChemReactorScreen"). Остальные 5
  граней по-прежнему на общих текстурах Thermal — это корректно, блок в мире их и использует.
- В мире (не в GUI) визуально **подтверждено разработчиком** — цветные иконки входа/выхода реально
  появляются на гранях блока при переключении конфигурации сторон в `ConfigPanel`
  (`ReconfigurableBakedModel`, см. раздел "Ресурсы и текстуры").
- **JEI-интеграция проверена визуально на реальном сервере модпака** (см. "JEI-интеграция" выше) —
  разработчик открыл страницу рецепта в JEI и по результатам попросил поправить раскладку слотов,
  что сделано (версия 1.0.2). Локальный `runClient` в этом проекте по-прежнему падает на Mixin-крашe
  KubeJS (см. жирный блок "Грабли: KubeJS не грузится в runClient" в начале файла) — это ограничение
  именно dev-окружения, в проде не воспроизводится, дальнейшая доработка раскладки JEI теперь тоже
  идёт через тест на реальном сервере, а не через `runClient`.

## Аудит и рефакторинг (сессия: полный код-ревью проекта)

Разработчик попросил детальный анализ всего проекта на баги/дюпы/архитектуру "как опытный Java-
разработчик". Прочитаны построчно все 16 Java-файлов и все ресурсы (build.gradle, gradle.properties,
mods.toml, pack.mcmeta, все JSON/lang/blockstate/model), после чего прогнан `compileJava` как
baseline (собирался чисто и до этой сессии).

**Главный вывод: логика самой машины (Tile/Container/Screen/RecipeManager/JEI/KubeJS) уже в хорошем
состоянии** — она много раз итеративно правилась в предыдущих сессиях (см. историю выше: позиционные
баки, `getBaseProcessTick()`, output-валидаторы, `ReconfigurableBakedModel` и т.д.), и на этот раз
новых функциональных багов в геймплейной логике реактора не найдено. Найдены и исправлены две
проблемы за пределами игровой логики — обе в конфигурации сборки:

1. **`pack.mcmeta` содержал нерабочий шаблон `${mod_id}`.** `tasks.processResources` в build.gradle
   применял `expand(...)` только к `META-INF/mods.toml` (через `filesMatching('META-INF/mods.toml')`),
   а `pack.mcmeta` в этот `filesMatching` не попадал вообще — значит placeholder `${mod_id}` в его
   поле `"description"` никогда не подставлялся, и в собранный jar буквально попадала строка
   `"${mod_id} resources"` (проверено: `./gradlew processResources` и просмотр
   `build/resources/main/pack.mcmeta` до фикса показывал ровно эту нераскрытую строку — это видно
   игроку в списке ресурспаков/датапаков в самой игре). Дополнительно сам ключ `mod_id` даже не был
   объявлен в `replaceProperties` — то есть подстановка была бы невозможна, даже если бы
   `filesMatching` для `pack.mcmeta` существовал.
   **Исправлено:** в `build.gradle` добавлен `mod_id: project.mod_id` в `replaceProperties` и
   отдельный блок `filesMatching('pack.mcmeta') { expand ... }`. Теперь `pack.mcmeta` в собранном
   jar корректно содержит `"thermal_addon_chemreactor resources"` (перепроверено сборкой).
2. **`gradle.properties` — нетронутый шаблон Forge MDK.** Значения `mod_id=examplemod`,
   `mod_name=Example Mod`, `mod_group_id=com.example.examplemod`, `mod_authors=YourNameHere,
   OtherNameHere`, `mod_description=Example mod description...` — это буквально дефолты генератора
   проекта, ни разу не отредактированные с момента `git init`. Сам `build.gradle` эти поля почти не
   использует (group/version/archivesBaseName у него захардкожены отдельно, `mods.toml` тоже пишется
   руками), поэтому раньше это был "мёртвый", но вводящий в заблуждение файл — а после фикса
   `pack.mcmeta` выше `mod_id` из этого файла стал реально использоваться сборкой.
   **Исправлено:** значения приведены в соответствие реальному проекту (`mod_id=
   thermal_addon_chemreactor`, `mod_group_id=com.sazdan.chemreactor`, `mod_authors=sazdan`, и т.д.).
3. **Дублированная магическая константа `DEFAULT_ENERGY = 4000`** — была независимо объявлена и в
   `ChemReactorRecipeSerializers` (энергия рецепта по умолчанию для `MachineRecipeSerializer`, если
   JSON не указывает `"energy"`), и в `ChemReactorRecipeManager` (передаётся в конструктор
   `AbstractManager`). Оба места должны совпадать по смыслу (то же самое "энергия рецепта по
   умолчанию"), но ничто не мешало им незаметно разойтись при будущей правке одного файла без
   другого. **Исправлено:** константа перенесена в одно место —
   `ChemReactorRecipeTypes.DEFAULT_ENERGY` (там же, где уже жил `ID_CHEM_REACTOR` — естественное
   место для констант, общих для типа рецепта), оба класса теперь ссылаются на неё.

**Осознанно НЕ тронуто (проверено, но не баг):**
- `validItem()` в `ChemReactorRecipeManager` — не позиционный (в отличие от `validFluid()`) —
  это осознанное решение по образцу `PressRecipeManager` (см. "Рецепты" выше), не забытая проверка.
- Три места с валидатором-заглушкой `(item) -> false`/`(fluid) -> false` для выходных слотов/бака
  в `ChemReactorTile` — да, дублирование одной строки, но выносить lambda-константу ради трёх
  одинаковых однострочников — это разработчику показалось бы избыточной абстракцией; оставлено как
  есть (согласуется с принципом "три похожие строки лучше преждевременной абстракции").
- `ChemReactorRecipeCategory.setRecipe()` берёт `outputChances.get(0)/.get(1)` без явной проверки
  границ — это тот же паттерн, что и у `BrewerRecipeCategory`/`PressRecipeCategory` самой Thermal
  (список `outputItemChances` гарантированно той же длины, что `outputItems`, это гарантирует
  сериализатор) — не баг, а установленный контракт CoFH.
- `README.txt`/`CREDITS.txt`/`changelog.txt`/`LICENSE.txt` в корне — это штатные файлы самого Forge
  MDK (описание форджа, а не мода), трогать не нужно, они не относятся к нашему коду.

**Проверка после рефакторинга:** `compileJava` — чисто, `./gradlew build` — `BUILD SUCCESSFUL`,
итоговый jar `build/libs/thermal_addon_chemreactor-1.0.0.jar` собран. Игровая логика (RF/аугменты/
рецепты/GUI/JEI/KubeJS) не менялась — правки не затрагивали ни один `.java`-файл с игровой логикой
кроме переноса одной константы, поэтому весь массив ранее подтверждённых в игре сценариев (см.
"Текущий статус" ниже) остаётся в силе без необходимости повторной проверки. `runClient` в рамках
этой сессии не запускался (не было такого запроса и по-прежнему требует явного "да" от разработчика
перед запуском, см. начало файла).

## Сессия: краш сервера на KubeJS-рецепте + лицевая грань в ConfigPanel (версия 1.0.3)

Разработчик принёс два бага с реального сервера модпака:

1. **Краш сервера при завершении цикла KubeJS-рецепта с одним занятым баком** (рецепт "500 mB latex
   + sulfur_dust + tar → ic2:rubber" через `event.recipes.thermal_addon_chemreactor.chem_reactor`;
   сам KubeJS-скрипт разработчика был корректен — бага была наша). Стек:
   `IllegalStateException: Can't modify the empty stack` из `FluidStack.setAmount` ←
   `FluidStorageCoFH.modify` ← `MachineBlockEntity.resolveInputs` ← `processFinish`. Полная цепочка
   причины и фикс — в разделах "ChemReactorTile" (`resolveInputs()`) и "Рецепты" (вычистка
   `EMPTY`-заглушек, `ChemReactorMachineRecipe`). Причина найдена декомпиляцией
   `MachineBlockEntity`/`SimpleMachineRecipe`/`BaseMachineRecipe`/`FluidStorageCoFH` через
   ForgeFlower (см. "Декомпиляция Thermal/CoFH для справки") — не гадали.
2. **Центральный квадратик в ConfigPanel показывал боковую текстуру вместо лицевой** — причина и
   фикс в разделе "ChemReactorScreen" (`createMachineConfigFace()`); заодно закрыт соответствующий
   пункт "Известных пробелов".

Попутно по просьбе разработчика **предметные входы сделаны непозиционными** (порядок раскладывания
реагентов по 2 входным слотам не важен; жидкости остались позиционными), любые слоты/баки могут
быть не заняты рецептом, а рецепты с превышением физических лимитов машины отклоняются с
warn-логом (детали — "Рецепты"). Инструкция по KubeJS переписана подробно (см. "Как правильно
взаимодействовать..."), включая объектную форму записи, которой пользовался разработчик.

**Проверено:** `compileJava` чисто, `./gradlew build` — `BUILD SUCCESSFUL`, собран
`thermal_addon_chemreactor-1.0.3.jar`. В игре (на сервере модпака) на момент записи ещё не
проверялось: краш-сценарий (рецепт с 1 жидкостью — завершение цикла), непозиционные предметы,
лицевая грань в ConfigPanel. `runClient` не запускался.

## Текущий статус

**Проект в целом готов** — все пункты плана разработки (см. ниже) закрыты, включая ассеты и
мягкую интеграцию с JEI/KubeJS. Из нового в предыдущей сессии (код-ревью) закрыто:
- Ассеты приведены в порядок: убраны 3 неиспользуемых текстуры блока и мёртвый lang-ключ
  `itemGroup.thermal_addon_chemreactor` (см. "Ресурсы и текстуры") — поведение не изменилось.
- GUI-код: исправлен неточный комментарий в `ChemReactorTile` про отсутствие выходной жидкости
  (жидкость на выходе есть, комментарий был устаревшим), координаты в `ChemReactorScreen` сделаны
  `public` для переиспользования в JEI-категории.
- JEI-интеграция и KubeJS-интеграция реализованы полностью (см. соответствующие разделы выше) —
  обе мягкие (`mandatory = false` / без прямых ссылок из основного кода мода), `compileJava`
  проходит чисто.
- Полный код-ревью/рефакторинг (см. раздел "Аудит и рефакторинг" выше) — исправлен нерабочий
  `${mod_id}` в `pack.mcmeta`, приведён в порядок мёртвый шаблонный `gradle.properties`, устранено
  дублирование константы `DEFAULT_ENERGY`. Собран и проверен финальный jar.

**Всё готово и проверено в игре (подтверждено разработчиком, до этой сессии):**
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
- Цветные иконки конфигурации сторон на гранях блока в мире (`ReconfigurableBakedModel`).
- **JEI-страница рецепта Chemical Reactor — проверена визуально на реальном сервере модпака**
  (не через локальный `runClient`, который по-прежнему падает на Mixin-крашe KubeJS, см. жирный блок
  в начале файла). По итогам теста раскладка слотов поправлена дважды (рамки баков "сверху над
  предметным слотом" вместо бок о бок, выходной бак — `TANK_MEDIUM`-размера, входная группа сдвинута
  на +22px) — версия мода после этих правок: **1.0.2**.
- Тестовый рецепт `dryrubber` обновлён по итогам того же теста: `light_oil` 500→100 mB, добавлен
  выходной `industrialforegoing:sludge` 100 mB (см. "Рецепты"/"Спецификация" выше).

**Проверено компиляцией, не подтверждено визуально в игре:**
- KubeJS-схема (`ChemReactorKubeJSPlugin`/`kubejs.plugins.txt`) — разработчик уже добавлял через
  неё рабочий рецепт на реальном сервере (значит схема и парсинг работают), но рецепт с одним
  занятым баком ронял сервер при завершении цикла — исправлено в 1.0.3 (см. раздел "Сессия: краш
  сервера..." выше), сам фикс на сервере ещё не перепроверен.
- Непозиционные предметные входы и лицевая грань в ConfigPanel (обе правки 1.0.3) — ждут теста на
  реальном сервере.

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
7. ✅ Assets — blockstate/модели/lang приведены в порядок, неиспользуемые файлы/ключи убраны
8. ✅ Мягкая интеграция с JEI и KubeJS — см. соответствующие разделы выше. JEI-страница проверена
   визуально на реальном сервере модпака, раскладка поправлена по итогам теста (версия 1.0.2, см.
   "Текущий статус"); KubeJS-схема пока проверена только тем, что мод грузится на сервере вместе с
   уже установленным там KubeJS — сам `event.recipes...chem_reactor(...)` отдельно не тестировали.
9. ⬜ Тест 1.0.3 на реальном сервере: (а) KubeJS-рецепт с одним занятым баком — полный цикл без
   краша (раньше ронял сервер); (б) предметы во входных слотах в любом порядке — рецепт собирается;
   (в) центральный квадратик ConfigPanel показывает лицевую текстуру реактора (и анимируется при
   активной машине).

**Дальше имеет смысл (не пронумеровано строго, на усмотрение):**
- Финальный уникальный арт GUI (фон/баки/прогресс/энергия) взамен текущих позаимствованных
  спрайтов — когда появится, менять только координаты/файл текстуры, код рендера трогать не нужно.
  То же самое касается фона JEI-категории (сейчас пустая канва + общие спрайты Thermal).
- Второй и последующие рецепты для реактора — предметы раскладываются в любом порядке, жидкости
  позиционны (1-я → бак 0, 2-я → бак 1), любые входы/выходы можно не использовать в рамках лимитов
  2+2 → 2+1 (см. "Рецепты").
