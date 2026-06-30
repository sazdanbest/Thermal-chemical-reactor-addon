package com.sazdan.chemreactor.init;

import cofh.thermal.lib.util.recipes.MachineRecipeSerializer;
import com.sazdan.chemreactor.ChemReactorAddon;
import com.sazdan.chemreactor.recipe.ChemReactorRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Регистрация RecipeSerializer — отвечает за чтение/запись рецепта (JSON <-> объект
// <-> сетевой пакет). MachineRecipeSerializer — готовый класс CoFH, который уже умеет
// парсить общий формат Thermal-рецептов (смешанный список "ingredients" из предметов
// и жидкостей, "result", "energy" и т.п.) — писать свой парсер JSON не нужно.
public class ChemReactorRecipeSerializers {

    // Энергия по умолчанию, если в JSON рецепта поле "energy" не указано явно.
    public static final int DEFAULT_ENERGY = 4000;

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, ChemReactorAddon.MOD_ID);

    public static final RegistryObject<MachineRecipeSerializer<ChemReactorRecipe>> CHEM_REACTOR_SERIALIZER =
            RECIPE_SERIALIZERS.register(ChemReactorRecipeTypes.ID_CHEM_REACTOR,
                    () -> new MachineRecipeSerializer<>(ChemReactorRecipe::new, DEFAULT_ENERGY));
}
