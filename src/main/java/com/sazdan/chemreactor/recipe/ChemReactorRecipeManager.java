package com.sazdan.chemreactor.recipe;

import cofh.core.util.helpers.FluidHelper;
import cofh.lib.api.fluid.IFluidStackHolder;
import cofh.lib.api.inventory.IItemStackHolder;
import cofh.lib.fluid.FluidIngredient;
import cofh.lib.util.crafting.ComparableItemStack;
import cofh.thermal.lib.util.managers.AbstractManager;
import cofh.thermal.lib.util.managers.IRecipeManager;
import cofh.thermal.lib.util.recipes.IThermalInventory;
import cofh.thermal.lib.util.recipes.ThermalRecipe;
import cofh.thermal.lib.util.recipes.internal.IMachineRecipe;
import cofh.thermal.lib.util.recipes.internal.SimpleMachineRecipe;
import com.sazdan.chemreactor.init.ChemReactorRecipeTypes;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

// ChemReactorRecipeManager — синглтон, который хранит все загруженные рецепты реактора
// и ищет подходящий по содержимому тайла. По образцу cofh.thermal.core...BottlerRecipeManager
// (1 предмет + 1 жидкость на входе), но расширен на 2 предметных слота + 2 бака,
// как у нашего реактора.
//
// Важно: совпадение позиционное, как и у CoFH-машин с несколькими входами (см. PressRecipeManager,
// слот "die" всегда slot[1]) — у первого рецепта это значит: предмет (sulfur_dust) кладётся
// в слот 0, latex — в бак 0, light_oil — в бак 1.
public class ChemReactorRecipeManager extends AbstractManager implements IRecipeManager {

    private static final ChemReactorRecipeManager INSTANCE = new ChemReactorRecipeManager();

    protected static final int DEFAULT_ENERGY = 4000;

    protected final Map<List<Object>, IMachineRecipe> recipeMap = new Object2ObjectOpenHashMap<>();
    protected final Set<ComparableItemStack> validItems = new ObjectOpenHashSet<>();
    protected final Set<Fluid> validFluids = new ObjectOpenHashSet<>();

    public static ChemReactorRecipeManager instance() {
        return INSTANCE;
    }

    private ChemReactorRecipeManager() {
        super(DEFAULT_ENERGY);
    }

    public boolean validItem(ItemStack item) {
        return validItems.contains(makeComparable(item));
    }

    public boolean validFluid(FluidStack fluid) {
        return validFluids.contains(fluid.getFluid());
    }

    protected void clear() {
        recipeMap.clear();
        validItems.clear();
        validFluids.clear();
    }

    // Из рецепта (Ingredient/FluidIngredient, у каждого может быть несколько вариантов
    // предметов/тегов) разворачиваем все конкретные комбинации в карту поиска —
    // как Brewer/Bottler/Press разворачивают свои Ingredient'ы.
    public void addRecipe(ThermalRecipe recipe) {
        List<Ingredient> items = recipe.getInputItems();
        List<FluidIngredient> fluids = recipe.getInputFluids();

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
                        addRecipe(recipe.getEnergy(), recipe.getXp(),
                                Arrays.asList(item0, item1), Arrays.asList(fluid0, fluid1),
                                recipe.getOutputItems(), recipe.getOutputItemChances(), recipe.getOutputFluids());
                    }
                }
            }
        }
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
        SimpleMachineRecipe recipe = new SimpleMachineRecipe(energy, experience, inputItems, inputFluids, outputItems, chance, outputFluids);

        for (ItemStack item : inputItems) {
            if (!item.isEmpty()) {
                validItems.add(makeComparable(item));
            }
        }
        for (FluidStack fluid : inputFluids) {
            if (!fluid.isEmpty()) {
                validFluids.add(fluid.getFluid());
            }
        }

        recipeMap.put(keyFromStacks(inputItems, inputFluids), recipe);
        return recipe;
    }

    protected List<Object> keyFromStacks(List<ItemStack> items, List<FluidStack> fluids) {
        List<Object> key = new ArrayList<>();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                key.add(makeComparable(item));
            }
        }
        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty()) {
                key.add(FluidHelper.fluidHashcodeNoTag(fluid));
            }
        }
        return key;
    }

    protected List<Object> keyFromInventory(List<? extends IItemStackHolder> inputSlots, List<? extends IFluidStackHolder> inputTanks) {
        List<Object> key = new ArrayList<>();
        for (IItemStackHolder slot : inputSlots) {
            if (!slot.isEmpty()) {
                key.add(makeComparable(slot.getItemStack()));
            }
        }
        for (IFluidStackHolder tank : inputTanks) {
            if (!tank.isEmpty()) {
                key.add(FluidHelper.fluidHashcodeNoTag(tank.getFluidStack()));
            }
        }
        return key;
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
}
