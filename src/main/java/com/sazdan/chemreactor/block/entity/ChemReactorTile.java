package com.sazdan.chemreactor.block.entity;

import cofh.lib.api.StorageGroup;
import cofh.lib.fluid.FluidStorageCoFH;
import cofh.lib.inventory.ItemStorageCoFH;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.lib.block.entity.MachineBlockEntity;
import com.sazdan.chemreactor.init.ChemReactorBlockEntities;
import com.sazdan.chemreactor.inventory.ChemReactorContainer;
import com.sazdan.chemreactor.recipe.ChemReactorRecipeManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;

// ChemReactorTile — это "мозг" блока: вся реальная логика машины живёт здесь,
// а не в ChemReactorBlock (блок — это просто физическая оболочка в мире).
//
// Наследуясь от MachineBlockEntity, мы автоматически получаем:
//   - управление RF-энергией (energyStorage)
//   - систему аугментов (слоты усиления, бонусы скорости/энергоэффективности)
//   - редстоун-контроль (можно настроить реакцию на сигнал редстоуна)
//   - конфигурацию сторон блока (вход/выход для авто-извлечения предметов трубами и т.п.)
//
public class ChemReactorTile extends MachineBlockEntity {

    public ChemReactorTile(BlockPos pos, BlockState state) {
        // Передаём BlockEntityType, зарегистрированный в ChemReactorBlockEntities.
        // .get() распаковывает RegistryObject в реальный объект BlockEntityType.
        super(ChemReactorBlockEntities.CHEMICAL_REACTOR.get(), pos, state);

        // --- Настройка слотов под предметы (item slots) ---
        // Согласно спецификации: 2 входных слота, 2 выходных слота.
        // StorageGroup.INPUT / OUTPUT — это то, что отличает "куда можно положить руками
        // и откуда труба может забрать" от "куда труба может только класть" ДЛЯ ТРУБ/ХОППЕРОВ —
        // но НЕ определяет, может ли игрок вручную положить предмет через GUI-слот (это отдельно
        // решает validator внутри самого ItemStorageCoFH/isItemValid()). Входные слоты ограничены
        // предикатом validItem() — принимают только предметы, которые реально используются хоть
        // одним загруженным рецептом реактора (плюс this.filter — фильтр-аугмент, если он когда-
        // нибудь появится). Тот же приём, что у MachineBrewerTile:
        // new ItemStorageCoFH((item) -> this.filter.valid(item) && BrewerRecipeManager.instance().validItem(item)).
        // Выходные слоты — валидатор (item) -> false: без него StorageGroup.OUTPUT сам по себе
        // ничего не запрещает, и игрок мог руками класть туда что угодно (баг, найден и исправлен
        // в этой сессии) — машина сама кладёт результат мимо validator'а, напрямую через
        // slot.setItemStack(...) в MachineBlockEntity.resolveOutputs(), так что это не мешает
        // машине писать в свои же выходные слоты, только ручной вставке игроком/трубой.
        this.inventory.addSlot(new ItemStorageCoFH((item) -> this.filter.valid(item) && ChemReactorRecipeManager.instance().validItem(item)), StorageGroup.INPUT);
        this.inventory.addSlot(new ItemStorageCoFH((item) -> this.filter.valid(item) && ChemReactorRecipeManager.instance().validItem(item)), StorageGroup.INPUT);
        this.inventory.addSlot(new ItemStorageCoFH((item) -> false), StorageGroup.OUTPUT);
        this.inventory.addSlot(new ItemStorageCoFH((item) -> false), StorageGroup.OUTPUT);

        // Charge-слот — зарядка энергией через предмет-батарейку (тот же chargeSlot, что уже есть
        // у MachineBlockEntity, просто раньше не был добавлен в инвентарь). StorageGroup.INTERNAL —
        // не участвует ни в позиционном поиске рецепта (не INPUT), ни в выдаче трубам (не OUTPUT).
        // Добавляется СРАЗУ после предметных слотов и ДО аугментов — как и у MachineBrewerTile
        // (inputSlot, chargeSlot, потом баки, потом addAugmentSlots() в самом конце).
        this.inventory.addSlot(this.chargeSlot, StorageGroup.INTERNAL);

        // --- Настройка баков под жидкости (fluid tanks) ---
        // 2 входных бака по 4000 mB, 1 выходной бак на 8000 mB — как в спецификации.
        // validFluid(0, ...)/validFluid(1, ...) — ПОЗИЦИОННО: бак 0 принимает только первую жидкость
        // рецепта, бак 1 — только вторую (см. ChemReactorRecipeManager). Раньше оба бака проверялись
        // одним общим набором "жидкость встречается хоть где-то в рецепте" — из-за этого при заливке
        // одной и той же жидкости (например, через несколько вёдер подряд или жидкостную ячейку) она
        // могла занять сразу оба входных бака, хотя по спецификации один бак = одна жидкость.
        // Выходной бак — валидатор (fluid) -> false, по той же причине, что и выходные предметные
        // слоты выше: игрок не должен вручную заливать/сливать через него, только машина.
        this.tankInv.addTank(new FluidStorageCoFH(4000, (fluid) -> this.filter.valid(fluid) && ChemReactorRecipeManager.instance().validFluid(0, fluid)), StorageGroup.INPUT);
        this.tankInv.addTank(new FluidStorageCoFH(4000, (fluid) -> this.filter.valid(fluid) && ChemReactorRecipeManager.instance().validFluid(1, fluid)), StorageGroup.INPUT);
        this.tankInv.addTank(new FluidStorageCoFH(8000, (fluid) -> false), StorageGroup.OUTPUT);

        // --- Аугмент-слоты ---
        // addAugmentSlots() дописывает N слотов в хвост this.inventory (StorageGroup.INTERNAL) —
        // ОБЯЗАТЕЛЬНО вызывать последним, после всех предметных слотов: AugmentableBlockEntity
        // считает аугменты последними augSize() слотами инвентаря (invSize() - augSize() .. invSize()),
        // на этом основано и сохранение (onReplaced/createItemStackTag), и load()/onInventoryChanged().
        // ThermalCoreConfig.machineAugments — стандартная настройка Thermal для машин (по умолчанию 4),
        // так же поступают MachineBrewerTile/MachineBottlerTile.
        this.addAugmentSlots(ThermalCoreConfig.machineAugments);

        // Обязательный финальный шаг после того как все слоты/баки добавлены —
        // без этого вызова инвентарь и баки не построят свои IItemHandler/IFluidHandler
        // (они создаются "лениво" через initHandlers(), и мы должны вызвать его
        // явно один раз сразу после настройки структуры).
        this.initHandlers();
    }

    // --- Тайминг и энергия машины ---
    // getBaseProcessTick() — это НЕ количество тиков цикла, а сколько FE тратится ЗА ОДИН ТИК
    // (см. MachineBlockEntity.processTick(): energyStorage.modify(-this.processTick);
    // this.process -= this.processTick, где this.processTick изначально равен this.baseProcessTick
    // = getBaseProcessTick()). Реальное число тиков до конца цикла = energy рецепта /
    // getBaseProcessTick(), округлённое вверх — это выяснилось и стоило разработчику "почти
    // мгновенного" цикла на предыдущей итерации (см. историю правок в этом файле раньше).
    // Итоговое требование разработчика: время цикла = время переплавки руды в Electric Furnace
    // самого Thermal (100 тиков = 5 секунд при её DEFAULT_ENERGY=2000 и базовой скорости
    // FurnaceRecipeManager.getBasePower()=20 — см. CLAUDE.md, "Третий раунд правок"), при этом
    // суммарная энергия рецепта реактора остаётся 4000 FE (не 2000, как у печи). Чтобы получить
    // ровно 100 тиков при 4000 FE: 4000 / 100 = 40 FE/тик — ровно делится, дробей не потребовалось.
    @Override
    protected int getBaseProcessTick() {
        return 40;
    }

    // --- Подключение рецептов ---
    // cacheRecipe() ищет подходящий рецепт через ChemReactorRecipeManager и заполняет
    // itemInputCounts/fluidInputCounts — это единственное, что нужно переопределить:
    // validateInputs()/validateOutputs()/processStart()/processFinish() в MachineBlockEntity
    // уже реализованы универсально (по числу слотов/баков), как и у MachineBottlerTile —
    // у нас тоже выход только предметами, без выходной жидкости.
    @Override
    protected boolean cacheRecipe() {
        this.curRecipe = ChemReactorRecipeManager.instance().getRecipe(this);
        if (this.curRecipe != null) {
            this.itemInputCounts = this.curRecipe.getInputItemCounts(this);
            this.fluidInputCounts = this.curRecipe.getInputFluidCounts(this);
        }
        return this.curRecipe != null;
    }

    // Хранилище опыта пока не используем (XP не выдаётся этой машиной) —
    // оставляем дефолтное значение родителя.
    // (getBaseXpStorage() не переопределяем, родитель даст 2500 по умолчанию,
    // но т.к. xpStorageFeature отключен по конфигу, на практике это не используется)

    // AugmentableBlockEntity (предок по иерархии) реализует интерфейс MenuProvider
    // целиком, включая getDisplayName() — переопределять его нам не нужно.
    // createMenu() — единственное, что нужно задать: какой Container открывать.
    // Сам вызов происходит из cofh.core.block.TileBlockCoFH.use() (родитель ChemReactorBlock)
    // через NetworkHooks.openScreen(...) — свой use() мы больше не пишем, см. ChemReactorBlock.
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return new ChemReactorContainer(containerId, this.level, this.worldPosition, playerInv, player);
    }
}