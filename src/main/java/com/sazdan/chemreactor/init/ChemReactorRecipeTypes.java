package com.sazdan.chemreactor.init;

import cofh.lib.util.recipes.SerializableRecipeType;
import com.sazdan.chemreactor.ChemReactorAddon;
import com.sazdan.chemreactor.recipe.ChemReactorRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Регистрация RecipeType — это "ярлык", по которому ванильный RecipeManager
// группирует JSON-рецепты из data/<modid>/recipes/. SerializableRecipeType — это
// готовый класс CoFH (cofh.lib.util.recipes), писать свою реализацию RecipeType не нужно.
public class ChemReactorRecipeTypes {

    public static final String ID_CHEM_REACTOR = "chem_reactor";

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, ChemReactorAddon.MOD_ID);

    public static final RegistryObject<RecipeType<ChemReactorRecipe>> CHEM_REACTOR =
            RECIPE_TYPES.register(ID_CHEM_REACTOR,
                    () -> new SerializableRecipeType<>(ChemReactorAddon.MOD_ID, ID_CHEM_REACTOR));
}
