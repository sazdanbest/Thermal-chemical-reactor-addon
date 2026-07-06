package com.sazdan.chemreactor.recipe;

import cofh.core.util.helpers.FluidHelper;
import cofh.core.util.helpers.ItemHelper;
import cofh.lib.api.fluid.IFluidStackHolder;
import cofh.lib.api.inventory.IItemStackHolder;
import cofh.lib.fluid.FluidIngredient;
import cofh.lib.util.crafting.ComparableItemStack;
import cofh.thermal.lib.util.managers.AbstractManager;
import cofh.thermal.lib.util.managers.IRecipeManager;
import cofh.thermal.lib.util.recipes.IMachineInventory;
import cofh.thermal.lib.util.recipes.IThermalInventory;
import cofh.thermal.lib.util.recipes.ThermalRecipe;
import cofh.thermal.lib.util.recipes.internal.IMachineRecipe;
import cofh.thermal.lib.util.recipes.internal.SimpleMachineRecipe;
import com.mojang.logging.LogUtils;
import com.sazdan.chemreactor.init.ChemReactorRecipeTypes;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// ChemReactorRecipeManager — синглтон, который хранит все загруженные рецепты реактора
// и ищет подходящий по содержимому тайла. По образцу cofh.thermal.core...BottlerRecipeManager
// (1 предмет + 1 жидкость на входе), но расширен на 2 предметных слота + 2 бака,
// как у нашего реактора.
//
// Правила совпадения (после переработки под "нестандартные" рецепты, в т.ч. из KubeJS):
//   - ЖИДКОСТИ — строго позиционные: первая жидкость в "ingredients" обязана быть в баке 0,
//     вторая — в баке 1 (как у CoFH-машин с несколькими входами, см. PressRecipeManager,
//     у которого слот "die" всегда slot[1]).
//   - ПРЕДМЕТЫ — НЕ позиционные: порядок раскладывания реагентов по двум входным слотам
//     не важен (sulfur+tar == tar+sulfur), ключ поиска сортирует предметы канонически.
//   - Любой вход может быть ПУСТЫМ: рецепт может использовать 0-2 предмета и 0-2 жидкости
//     (но хотя бы что-то одно). Неиспользуемые рецептом слоты/баки должны быть пустыми
//     в машине — иначе содержимое не совпадает с рецептом и цикл не начнётся.
public class ChemReactorRecipeManager extends AbstractManager implements IRecipeManager {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final ChemReactorRecipeManager INSTANCE = new ChemReactorRecipeManager();

    // Физические ограничения машины (см. ChemReactorTile): 2 входных предметных слота,
    // 2 входных бака, 2 выходных предметных слота, 1 выходной бак. Рецепты, объявляющие
    // больше — отклоняются целиком с warn-логом (см. addRecipe(ThermalRecipe)): лучше
    // громко не загрузить рецепт, чем молча проигнорировать часть его ингредиентов.
    public static final int MAX_INPUT_ITEMS = 2;
    public static final int MAX_INPUT_FLUIDS = 2;
    public static final int MAX_OUTPUT_ITEMS = 2;
    public static final int MAX_OUTPUT_FLUIDS = 1;

    protected final Map<List<Object>, IMachineRecipe> recipeMap = new Object2ObjectOpenHashMap<>();
    protected final Set<ComparableItemStack> validItems = new ObjectOpenHashSet<>();
    // Позиционные наборы валидных жидкостей — отдельно для бака 0 и бака 1 (по образцу
    // PressRecipeManager.validInputs/validDies, у которого слот 0 ("input") и слот 1 ("die")
    // тоже валидируются раздельно). Раньше был один общий Set<Fluid> на оба бака — из-за этого
    // любой бак принимал любую жидкость рецепта, и при заливке через ведро/жидкостную ячейку
    // одна и та же жидкость могла одновременно занять оба входных бака. Позиционность здесь
    // жёстко привязана к тому же порядку, что и в JSON рецепта/ChemReactorRecipe (см. "Рецепты"):
    // первая жидкость в "ingredients" — только бак 0, вторая — только бак 1.
    protected final Set<Fluid> validFluidsTank0 = new ObjectOpenHashSet<>();
    protected final Set<Fluid> validFluidsTank1 = new ObjectOpenHashSet<>();
    // "Инициализация рецепта" жидкостью: как только один из входных баков занят, второй бак
    // принимает только жидкости, которые встречаются С НЕЙ В ПАРЕ хоть в одном рецепте
    // (латекс в баке 0 -> бак 1 примет только light_oil, если это единственный рецепт с латексом).
    // Никакого хранимого состояния у тайла нет — валидатор бака в момент заливки смотрит на
    // текущее содержимое соседнего бака (см. ChemReactorTile), поэтому опустевший/очищенный
    // Alt+Shift+кликом бак автоматически снимает ограничение, ничего сбрасывать не нужно.
    //   validPairsTank1ByTank0: жидкость бака 0 -> все жидкости, допустимые в баке 1 рядом с ней;
    //   validPairsTank0ByTank1: обратное направление (жидкость бака 1 -> допустимые в баке 0).
    // Рецепт с ОДНОЙ жидкостью пары не даёт: пока его жидкость в баке 0, второй бак не примет
    // ничего (у такого рецепта второй бак обязан быть пустым — иначе цикл всё равно не начнётся).
    protected final Map<Fluid, Set<Fluid>> validPairsTank1ByTank0 = new Object2ObjectOpenHashMap<>();
    protected final Map<Fluid, Set<Fluid>> validPairsTank0ByTank1 = new Object2ObjectOpenHashMap<>();

    public static ChemReactorRecipeManager instance() {
        return INSTANCE;
    }

    private ChemReactorRecipeManager() {
        super(ChemReactorRecipeTypes.DEFAULT_ENERGY);
    }

    public boolean validItem(ItemStack item) {
        return validItems.contains(makeComparable(item));
    }

    // tankIndex — 0 или 1, соответствует индексу входного бака в ChemReactorTile.tankInv
    // (0 и 1 — входные, 2 — выходной, для него этот метод не вызывается).
    // Базовая (без контекста соседнего бака) проверка: жидкость встречается на этой позиции
    // хоть в одном рецепте. Используется, когда соседний бак пуст — см. перегрузку ниже.
    public boolean validFluid(int tankIndex, FluidStack fluid) {
        if (fluid.isEmpty()) {
            return false;
        }
        Set<Fluid> validFluids = tankIndex == 0 ? validFluidsTank0 : validFluidsTank1;
        return validFluids.contains(fluid.getFluid());
    }

    // Проверка с учётом "инициализации рецепта" содержимым соседнего входного бака:
    //   - соседний бак пуст -> реактор ещё не "закреплён" за рецептом, принимаем любую жидкость,
    //     валидную для этой позиции (старое поведение);
    //   - соседний бак занят -> принимаем только жидкость, образующую с ней пару хоть в одном
    //     рецепте (см. validPairsTank0ByTank1/validPairsTank1ByTank0 выше). Направление пары
    //     позиционное: для бака 0 партнёр ищется среди "первых" жидкостей рецептов с "второй"
    //     жидкостью из бака 1, и наоборот.
    public boolean validFluid(int tankIndex, FluidStack fluid, FluidStack otherTankFluid) {
        if (fluid.isEmpty()) {
            return false;
        }
        if (otherTankFluid.isEmpty()) {
            return validFluid(tankIndex, fluid);
        }
        Map<Fluid, Set<Fluid>> pairs = tankIndex == 0 ? validPairsTank0ByTank1 : validPairsTank1ByTank0;
        Set<Fluid> allowed = pairs.get(otherTankFluid.getFluid());
        return allowed != null && allowed.contains(fluid.getFluid());
    }

    protected void clear() {
        recipeMap.clear();
        validItems.clear();
        validFluidsTank0.clear();
        validFluidsTank1.clear();
        validPairsTank1ByTank0.clear();
        validPairsTank0ByTank1.clear();
    }

    // Из рецепта (Ingredient/FluidIngredient, у каждого может быть несколько вариантов
    // предметов/тегов) разворачиваем все конкретные комбинации в карту поиска —
    // как Brewer/Bottler/Press разворачивают свои Ingredient'ы.
    public void addRecipe(ThermalRecipe recipe) {
        List<Ingredient> items = recipe.getInputItems();
        List<FluidIngredient> fluids = recipe.getInputFluids();

        // Валидация под физические слоты машины. Рецепт с лишними ингредиентами раньше
        // молча терял всё сверх первых двух — теперь отклоняется целиком с понятным логом
        // (актуально для рецептов из KubeJS, где легко опечататься).
        //
        // Выходные ПРЕДМЕТЫ считаются по числу РАЗНЫХ предметов, а не по числу записей:
        // несколько выходных записей одного и того же предмета не занимают несколько слотов —
        // MachineBlockEntity.resolveOutputs() докладывает совпадающий предмет в уже занятый
        // слот (grow), а не в новый. Это и даёт "случайное количество" одного предмета: запись
        // вида "1 шт. + 50% + 25% того же предмета" бросает три независимых кубика и складывает
        // выпавшее в один слот (итог 1-3 шт.). По числу записей такой рецепт (3 > 2) ложно
        // отклонялся бы как "слишком много результатов", хотя ему нужен всего один слот.
        if (items.size() > MAX_INPUT_ITEMS || fluids.size() > MAX_INPUT_FLUIDS
                || distinctOutputItemSlots(recipe.getOutputItems()) > MAX_OUTPUT_ITEMS
                || recipe.getOutputFluids().size() > MAX_OUTPUT_FLUIDS) {
            LOGGER.warn("Chemical Reactor recipe {} rejected: too many ingredients/results "
                            + "(max {} input items, {} input fluids, {} output items, {} output fluid)",
                    recipe.getId(), MAX_INPUT_ITEMS, MAX_INPUT_FLUIDS, MAX_OUTPUT_ITEMS, MAX_OUTPUT_FLUIDS);
            return;
        }
        if (items.isEmpty() && fluids.isEmpty()) {
            LOGGER.warn("Chemical Reactor recipe {} rejected: it has no inputs at all", recipe.getId());
            return;
        }

        List<ItemStack> item0Options = items.size() > 0
                ? Arrays.asList(items.get(0).getItems()) : List.of(ItemStack.EMPTY);
        List<ItemStack> item1Options = items.size() > 1
                ? Arrays.asList(items.get(1).getItems()) : List.of(ItemStack.EMPTY);
        List<FluidStack> fluid0Options = fluids.size() > 0
                ? Arrays.asList(fluids.get(0).getFluids()) : List.of(FluidStack.EMPTY);
        List<FluidStack> fluid1Options = fluids.size() > 1
                ? Arrays.asList(fluids.get(1).getFluids()) : List.of(FluidStack.EMPTY);

        for (ItemStack item0 : item0Options) {
            for (ItemStack item1 : item1Options) {
                for (FluidStack fluid0 : fluid0Options) {
                    for (FluidStack fluid1 : fluid1Options) {
                        // ВАЖНО: пустые заглушки (EMPTY) из списков вычищаются ЗДЕСЬ и не
                        // попадают внутрь SimpleMachineRecipe. Раньше рецепт с одной жидкостью
                        // хранил inputFluids = [latex, FluidStack.EMPTY], из-за чего
                        // getInputFluidCounts() отдавал лишний счётчик "0" для пустого бака,
                        // а MachineBlockEntity.resolveInputs() вызывал на нём modify(-0) —
                        // FluidStack.EMPTY.setAmount() бросает IllegalStateException
                        // ("Can't modify the empty stack") и валит сервер при завершении цикла.
                        List<ItemStack> inputItems = new ArrayList<>(2);
                        if (!item0.isEmpty()) {
                            inputItems.add(item0);
                        }
                        if (!item1.isEmpty()) {
                            inputItems.add(item1);
                        }
                        List<FluidStack> inputFluids = new ArrayList<>(2);
                        if (!fluid0.isEmpty()) {
                            inputFluids.add(fluid0);
                        }
                        if (!fluid1.isEmpty()) {
                            inputFluids.add(fluid1);
                        }
                        addRecipe(recipe.getEnergy(), recipe.getXp(), inputItems, inputFluids,
                                recipe.getOutputItems(), recipe.getOutputItemChances(), recipe.getOutputFluids());
                    }
                }
            }
        }
    }

    // Сколько ФИЗИЧЕСКИХ выходных предметных слотов реально займёт рецепт. Совпадающие
    // (itemsEqualWithTags — та же проверка, по которой resolveOutputs() докладывает предмет
    // в существующий слот) записи считаются за один слот; пустые заглушки игнорируются.
    // Используется только для валидации лимита MAX_OUTPUT_ITEMS — сам список выходов
    // (со всеми записями и их шансами) в рецепт передаётся без изменений.
    protected static int distinctOutputItemSlots(List<ItemStack> outputs) {
        List<ItemStack> distinct = new ArrayList<>(outputs.size());
        for (ItemStack out : outputs) {
            if (out.isEmpty()) {
                continue;
            }
            boolean seen = false;
            for (ItemStack d : distinct) {
                if (ItemHelper.itemsEqualWithTags(d, out)) {
                    seen = true;
                    break;
                }
            }
            if (!seen) {
                distinct.add(out);
            }
        }
        return distinct.size();
    }

    protected IMachineRecipe addRecipe(int energy, float experience, List<ItemStack> inputItems, List<FluidStack> inputFluids,
                                        List<ItemStack> outputItems, List<Float> chance, List<FluidStack> outputFluids) {
        if (outputItems.isEmpty() && outputFluids.isEmpty()) {
            return null;
        }
        for (ItemStack stack : outputItems) {
            if (stack.isEmpty()) {
                return null;
            }
        }
        for (FluidStack stack : outputFluids) {
            if (stack.isEmpty()) {
                return null;
            }
        }

        energy = (int) ((float) energy * getDefaultScale());
        ChemReactorMachineRecipe recipe = new ChemReactorMachineRecipe(energy, experience, inputItems, inputFluids, outputItems, chance, outputFluids);

        for (ItemStack item : inputItems) {
            validItems.add(makeComparable(item));
        }
        // Позиционно: inputFluids.get(0) — только бак 0, inputFluids.get(1) — только бак 1.
        if (inputFluids.size() > 0) {
            validFluidsTank0.add(inputFluids.get(0).getFluid());
        }
        if (inputFluids.size() > 1) {
            validFluidsTank1.add(inputFluids.get(1).getFluid());
            // Пара для "инициализации рецепта" (см. validPairsTank1ByTank0/...): двухжидкостный
            // рецепт разрешает своей второй жидкости заливаться рядом с первой и наоборот.
            Fluid fluid0 = inputFluids.get(0).getFluid();
            Fluid fluid1 = inputFluids.get(1).getFluid();
            validPairsTank1ByTank0.computeIfAbsent(fluid0, f -> new ObjectOpenHashSet<>()).add(fluid1);
            validPairsTank0ByTank1.computeIfAbsent(fluid1, f -> new ObjectOpenHashSet<>()).add(fluid0);
        }

        recipeMap.put(keyFromStacks(inputItems, inputFluids), recipe);
        return recipe;
    }

    // --- Ключ поиска рецепта ---
    // Ключ = [предметы, отсортированные канонически] + [хэш жидкости бака 0, хэш жидкости бака 1].
    //   - Предметы сортируются (см. sortedComparables), поэтому порядок раскладывания реагентов
    //     по входным слотам НЕ важен.
    //   - Жидкости — по одной ячейке на КАЖДЫЙ бак (0 = бак пуст), поэтому позиция жидкости
    //     важна: рецепт [latex, -] совпадает только когда latex в баке 0, а бак 1 пуст.
    //     Раньше пустые баки просто пропускались — из-за этого жидкость из бака 1 могла
    //     "съехать" на позицию бака 0 в ключе, и рецепт совпадал при физически неверной
    //     раскладке (а resolveInputs() потом списывал бы из не того бака).

    protected List<Object> keyFromStacks(List<ItemStack> items, List<FluidStack> fluids) {
        List<ComparableItemStack> comparables = new ArrayList<>(items.size());
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                comparables.add(makeComparable(item));
            }
        }
        List<Object> key = new ArrayList<>(comparables.size() + MAX_INPUT_FLUIDS);
        key.addAll(sortedComparables(comparables));
        for (int k = 0; k < MAX_INPUT_FLUIDS; ++k) {
            key.add(k < fluids.size() && !fluids.get(k).isEmpty()
                    ? FluidHelper.fluidHashcodeNoTag(fluids.get(k)) : 0);
        }
        return key;
    }

    protected List<Object> keyFromInventory(List<? extends IItemStackHolder> inputSlots, List<? extends IFluidStackHolder> inputTanks) {
        List<ComparableItemStack> comparables = new ArrayList<>(inputSlots.size());
        for (IItemStackHolder slot : inputSlots) {
            if (!slot.isEmpty()) {
                comparables.add(makeComparable(slot.getItemStack()));
            }
        }
        List<Object> key = new ArrayList<>(comparables.size() + MAX_INPUT_FLUIDS);
        key.addAll(sortedComparables(comparables));
        for (int k = 0; k < MAX_INPUT_FLUIDS; ++k) {
            key.add(k < inputTanks.size() && !inputTanks.get(k).isEmpty()
                    ? FluidHelper.fluidHashcodeNoTag(inputTanks.get(k).getFluidStack()) : 0);
        }
        return key;
    }

    // Канонический порядок предметов в ключе — по hashCode ComparableItemStack (одинаковые
    // предметы дают одинаковый hashCode, поэтому рецептная и инвентарная стороны сортируются
    // согласованно). Этим и достигается нечувствительность к порядку входных слотов.
    protected static List<ComparableItemStack> sortedComparables(List<ComparableItemStack> comparables) {
        if (comparables.size() > 1) {
            comparables.sort(Comparator.comparingInt(Object::hashCode));
        }
        return comparables;
    }

    @Override
    public IMachineRecipe getRecipe(IThermalInventory inventory) {
        List<? extends IItemStackHolder> inputSlots = inventory.inputSlots();
        List<? extends IFluidStackHolder> inputTanks = inventory.inputTanks();
        if (inputSlots.isEmpty() && inputTanks.isEmpty()) {
            return null;
        }
        return recipeMap.get(keyFromInventory(inputSlots, inputTanks));
    }

    @Override
    public List<IMachineRecipe> getRecipeList() {
        return new ArrayList<>(recipeMap.values());
    }

    @Override
    public void refresh(RecipeManager recipeManager) {
        clear();
        // byType(RecipeType) у RecipeManager в official-маппингах private — используем
        // публичный getAllRecipesFor(...), который внутри вызывает тот же byType().
        for (ChemReactorRecipe recipe : recipeManager.getAllRecipesFor(ChemReactorRecipeTypes.CHEM_REACTOR.get())) {
            addRecipe(recipe);
        }
    }

    // Внутренний рецепт реактора. Отличие от SimpleMachineRecipe — счётчики потребления
    // выровнены ПО РЕАЛЬНЫМ СЛОТАМ/БАКАМ инвентаря, а не по списку ингредиентов рецепта:
    //   - SimpleMachineRecipe.getInputItemCounts() возвращает [кол-во ингредиента 0, ингредиента 1,
    //     ...] в порядке объявления рецепта, а MachineBlockEntity.validateInputs()/resolveInputs()
    //     применяет i-й счётчик к i-му СЛОТУ — корректно, только если раскладка в машине буквально
    //     совпадает с порядком объявления. У нас предметы разрешено класть в любом порядке,
    //     поэтому счётчик каждого ингредиента ищет "свой" слот по совпадению предмета.
    //   - Для жидкостей порядок жёсткий (ингредиент k = бак k), поэтому счётчики просто
    //     раскладываются по индексам баков; незадействованные баки получают 0
    //     (ChemReactorTile.resolveInputs() нулевые счётчики пропускает — см. там же почему
    //     это критично для пустых баков).
    protected static class ChemReactorMachineRecipe extends SimpleMachineRecipe {

        public ChemReactorMachineRecipe(int energy, float experience,
                                         List<ItemStack> inputItems, List<FluidStack> inputFluids,
                                         List<ItemStack> outputItems, List<Float> chance, List<FluidStack> outputFluids) {
            super(energy, experience, inputItems, inputFluids, outputItems, chance, outputFluids);
        }

        // По образцу BaseMachineRecipe.getInputItemCounts() (сопоставление по содержимому
        // инвентаря), но с флагами used[] — иначе два одинаковых требования рецепта
        // "прилипли" бы к одному и тому же слоту.
        @Override
        public List<Integer> getInputItemCounts(IMachineInventory inventory) {
            List<? extends IItemStackHolder> slots = inventory.inputSlots();
            if (this.inputItems.isEmpty() || slots.isEmpty()) {
                return Collections.emptyList();
            }
            int[] ret = new int[slots.size()];
            boolean[] used = new boolean[slots.size()];
            for (ItemStack input : this.inputItems) {
                for (int j = 0; j < ret.length; ++j) {
                    if (!used[j] && ItemHelper.itemsEqual(input, slots.get(j).getItemStack())) {
                        used[j] = true;
                        ret[j] = input.getCount();
                        break;
                    }
                }
            }
            return IntStream.of(ret).boxed().collect(Collectors.toList());
        }

        // Жидкости позиционны: ингредиент k живёт строго в баке k (это гарантирует ключ
        // поиска — рецепт вообще не совпадёт при другой раскладке), поэтому счётчики
        // раскладываются по индексам напрямую.
        @Override
        public List<Integer> getInputFluidCounts(IMachineInventory inventory) {
            List<? extends IFluidStackHolder> tanks = inventory.inputTanks();
            if (this.inputFluids.isEmpty() || tanks.isEmpty()) {
                return Collections.emptyList();
            }
            int[] ret = new int[tanks.size()];
            for (int k = 0; k < this.inputFluids.size() && k < ret.length; ++k) {
                ret[k] = this.inputFluids.get(k).getAmount();
            }
            return IntStream.of(ret).boxed().collect(Collectors.toList());
        }
    }
}
