package com.sazdan.chemreactor.block.entity;

import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.ItemHelper;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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

    // Входные баки вынесены в поля, потому что их валидаторы перекрёстные: валидатор бака 0
    // в момент заливки читает ТЕКУЩЕЕ содержимое бака 1 и наоборот ("инициализация рецепта",
    // см. ChemReactorRecipeManager.validFluid(int, FluidStack, FluidStack)). Лямбда-валидатор
    // вычисляется лениво (при fill()), а не при создании бака, поэтому ссылка на соседний бак,
    // который в момент создания первого ещё не присвоен, безопасна — к моменту первого fill()
    // оба поля давно инициализированы конструктором. Поля НЕ final намеренно: blank-final
    // запрещено читать в лямбде до присвоения (javac: "might not have been initialized"),
    // а валидатору бака 0 нужна ссылка на ещё не созданный бак 1.
    protected FluidStorageCoFH inputTank0;
    protected FluidStorageCoFH inputTank1;

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
        // Валидаторы входных баков — ПОЗИЦИОННЫЕ и ПЕРЕКРЁСТНЫЕ ("инициализация рецепта"):
        //   - пока ОБА бака пусты, каждый принимает любую жидкость, валидную для его позиции
        //     (бак 0 — только "первые" жидкости рецептов, бак 1 — только "вторые");
        //   - как только один бак занят, второй принимает ТОЛЬКО жидкость, встречающуюся с ней
        //     в паре хоть в одном рецепте (латекс в баке 0 -> бак 1 примет только light_oil).
        // Состояние нигде не хранится: валидатор в момент fill() читает текущее содержимое
        // соседнего бака, поэтому опустевший (выработанный циклом или очищенный Alt+Shift+кликом
        // по баку в GUI — это штатный CoFH StorageClearPacket, уже подключён в ChemReactorScreen
        // через GuiHelper.setClearable) бак автоматически снимает ограничение. Загрузке из NBT
        // валидатор не мешает — FluidStorageCoFH.read()/setFluidStack() идут мимо isFluidValid().
        // Выходной бак — валидатор (fluid) -> false, по той же причине, что и выходные предметные
        // слоты выше: игрок не должен вручную заливать/сливать через него, только машина.
        this.inputTank0 = new FluidStorageCoFH(4000, (fluid) -> this.filter.valid(fluid)
                && ChemReactorRecipeManager.instance().validFluid(0, fluid, this.inputTank1.getFluidStack()));
        this.inputTank1 = new FluidStorageCoFH(4000, (fluid) -> this.filter.valid(fluid)
                && ChemReactorRecipeManager.instance().validFluid(1, fluid, this.inputTank0.getFluidStack()));
        this.tankInv.addTank(this.inputTank0, StorageGroup.INPUT);
        this.tankInv.addTank(this.inputTank1, StorageGroup.INPUT);
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
    // itemInputCounts/fluidInputCounts. validateInputs()/processStart()/processFinish()
    // в MachineBlockEntity реализованы универсально (по числу слотов/баков) и не переопределяются;
    // validateOutputs() и resolveInputs() переопределены — см. комментарии к ним выше/ниже.
    @Override
    protected boolean cacheRecipe() {
        this.curRecipe = ChemReactorRecipeManager.instance().getRecipe(this);
        if (this.curRecipe != null) {
            this.itemInputCounts = this.curRecipe.getInputItemCounts(this);
            this.fluidInputCounts = this.curRecipe.getInputFluidCounts(this);
        }
        return this.curRecipe != null;
    }

    // --- Проверка, что результатам есть куда лечь (фикс шансовых рецептов) ---
    // Родительский MachineBlockEntity.validateOutputs() резервирует ПО ОДНОМУ выходному слоту
    // НА КАЖДУЮ ЗАПИСЬ рецепта (used[i] = true на первой же подошедшей ячейке), НЕ складывая
    // одинаковые предметы. Шансовый рецепт "случайное количество" из нескольких записей одного
    // предмета (например [rubber x2, rubber chance:0.5, rubber chance:0.5]) требовал бы у него
    // 3 свободных слота, а их всего 2 -> validateOutputs() всегда false -> canProcessStart()
    // никогда не проходит, машина просто не стартует (без ошибок в логе). При этом сама выдача
    // (resolveOutputs()) одинаковые предметы честно складывает в ОДИН слот (grow) — расходились
    // именно проверка и выдача. Наша версия сначала ГРУППИРУЕТ записи по одинаковым предметам
    // (тот же критерий ItemHelper.itemsEqualWithTags, что и в resolveOutputs), а дальше выполняет
    // родительский алгоритм по группам: одна группа = один физический слот. Валидация лимита
    // "не больше 2 РАЗНЫХ предметов" уже сделана менеджером при загрузке рецепта
    // (см. ChemReactorRecipeManager.distinctOutputItemSlots), так что группы всегда помещаются.
    // Жидкостная часть — без изменений, скопирована из родителя (метод нельзя дополнить частично).
    @Override
    protected boolean validateOutputs() {
        if (curRecipe == null && !cacheRecipe()) {
            return false;
        }
        // Группируем выходные записи по одинаковым предметам, сохраняя порядок первых вхождений
        // (первая группа обычно содержит гарантированную запись — это важно для ветки
        // secondaryNullFeature ниже, как у родителя j == 0).
        List<ItemStack> recipeOutputItems = curRecipe.getOutputItems(this);
        List<ItemStack> groupedOutputs = new ArrayList<>(recipeOutputItems.size());
        for (ItemStack recipeOutput : recipeOutputItems) {
            boolean seen = false;
            for (ItemStack grouped : groupedOutputs) {
                if (ItemHelper.itemsEqualWithTags(grouped, recipeOutput)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                groupedOutputs.add(recipeOutput);
            }
        }

        // Дальше — родительский алгоритм, но по группам вместо сырых записей.
        var slotOutputs = outputSlots();
        boolean[] used = new boolean[slotOutputs.size()];
        for (int j = 0; j < groupedOutputs.size(); ++j) {
            ItemStack recipeOutput = groupedOutputs.get(j);
            boolean matched = false;
            for (int i = 0; i < slotOutputs.size(); ++i) {
                if (!used[i]) {
                    ItemStack output = slotOutputs.get(i).getItemStack();
                    if (output.getCount() < output.getMaxStackSize() && ItemHelper.itemsEqualWithTags(output, recipeOutput)) {
                        used[i] = true;
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                for (int i = 0; i < slotOutputs.size(); ++i) {
                    if (!used[i] && slotOutputs.get(i).isEmpty()) {
                        used[i] = true;
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched && (j == 0 || !secondaryNullFeature)) {
                return false;
            }
        }

        var tankOutputs = outputTanks();
        List<FluidStack> recipeOutputFluids = curRecipe.getOutputFluids(this);
        boolean[] usedTanks = new boolean[tankOutputs.size()];
        for (FluidStack recipeOutput : recipeOutputFluids) {
            boolean matched = false;
            for (int i = 0; i < tankOutputs.size(); ++i) {
                FluidStack output = tankOutputs.get(i).getFluidStack();
                if (!usedTanks[i] && tankOutputs.get(i).getSpace() >= recipeOutput.getAmount() && FluidHelper.fluidsEqual(output, recipeOutput)) {
                    usedTanks[i] = true;
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                for (int i = 0; i < tankOutputs.size(); ++i) {
                    if (!usedTanks[i] && tankOutputs.get(i).isEmpty()) {
                        usedTanks[i] = true;
                        matched = true;
                        break;
                    }
                }
            }
            if (!matched) {
                return false;
            }
        }
        return true;
    }

    // --- Списание входов по завершении цикла ---
    // Родительский MachineBlockEntity.resolveInputs() применяет i-й счётчик из
    // itemInputCounts/fluidInputCounts к i-му слоту/баку БЕЗУСЛОВНО, даже если счётчик равен 0,
    // а слот/бак пуст. Для предметов это безобидно (consume(0) на пустом ItemStack ничего не
    // делает), а вот FluidStorageCoFH.modify(0) на пустом баке вызывает
    // FluidStack.EMPTY.setAmount() -> IllegalStateException "Can't modify the empty stack"
    // и роняет сервер тиком тайла ровно в момент завершения цикла. У наших рецептов счётчики
    // выровнены по всем слотам/бакам машины (см. ChemReactorMachineRecipe в
    // ChemReactorRecipeManager), т.е. нули для незадействованных входов — норма, а не ошибка:
    // рецепт имеет право занимать не все входы (например, 1 жидкость + 2 предмета из KubeJS).
    @Override
    protected void resolveInputs() {
        var slots = inputSlots();
        for (int i = 0; i < itemInputCounts.size() && i < slots.size(); ++i) {
            int count = itemInputCounts.get(i);
            if (count > 0) {
                slots.get(i).consume(count);
            }
        }
        var tanks = inputTanks();
        for (int i = 0; i < fluidInputCounts.size() && i < tanks.size(); ++i) {
            int count = fluidInputCounts.get(i);
            if (count > 0) {
                tanks.get(i).modify(-count);
            }
        }
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