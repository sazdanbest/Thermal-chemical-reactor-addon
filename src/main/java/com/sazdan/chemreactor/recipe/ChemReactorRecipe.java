package com.sazdan.chemreactor.recipe;

import cofh.lib.fluid.FluidIngredient;
import cofh.thermal.lib.util.recipes.ThermalRecipe;
import com.sazdan.chemreactor.init.ChemReactorRecipeSerializers;
import com.sazdan.chemreactor.init.ChemReactorRecipeTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

// ChemReactorRecipe — JSON-описание рецепта реактора (вход/выход/энергия).
// Сама обработка (тики, проверка инвентаря) этим классом не занимается —
// это просто неизменяемые данные рецепта. Логика поиска и валидации —
// в ChemReactorRecipeManager, по образцу cofh.thermal.core...BrewerRecipe.
public class ChemReactorRecipe extends ThermalRecipe {

    public ChemReactorRecipe(ResourceLocation id, int energy, float xp,
                              List<Ingredient> inputItems, List<FluidIngredient> inputFluids,
                              List<ItemStack> outputItems, List<Float> outputItemChances,
                              List<FluidStack> outputFluids) {
        super(id, energy, xp, inputItems, inputFluids, outputItems, outputItemChances, outputFluids);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ChemReactorRecipeSerializers.CHEM_REACTOR_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ChemReactorRecipeTypes.CHEM_REACTOR.get();
    }
}
