package com.sazdan.chemreactor.client.compat.jei;

import com.sazdan.chemreactor.ChemReactorAddon;
import com.sazdan.chemreactor.client.ChemReactorScreen;
import com.sazdan.chemreactor.init.ChemReactorBlocks;
import com.sazdan.chemreactor.init.ChemReactorRecipeTypes;
import com.sazdan.chemreactor.recipe.ChemReactorRecipe;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

// Мягкая (опциональная) интеграция с JEI — показывает рецепты Chemical Reactor во вкладке
// рецептов. Устроено ровно как TExpJeiPlugin у самой Thermal (cofh.thermal.expansion.compat.jei,
// декомпилировано и проверено при разработке) — один RecipeType, одна категория, сам реактор как
// "катализатор" (иконка "сделано в..."), плюс клик по индикатору реакции на настоящем экране
// открывает список рецептов в JEI (как и у всех родных машин Thermal).
//
// Класс ссылается на классы JEI (mezz.jei.api.*), поэтому его нельзя вызывать напрямую из своего
// кода — иначе без установленного JEI игра упадёт с NoClassDefFoundError. Загрузку этого класса
// делает сам JEI: он сканирует все моды на classpath в поисках классов с аннотацией @JeiPlugin
// (через общий ASM-сканер аннотаций Forge, который не требует резолва самих классов) и создаёт
// найденные — но только если сам JEI установлен и реально это делает. Наш код этот класс никогда
// не создаёт и не импортирует — та же схема "мягкой" зависимости, что и у KubeJS-интеграции
// (см. integration/kubejs/ChemReactorKubeJSPlugin).
@JeiPlugin
public class ChemReactorJeiPlugin implements IModPlugin {

    public static final RecipeType<ChemReactorRecipe> CHEM_REACTOR_TYPE =
            new RecipeType<>(ChemReactorRecipeTypes.CHEM_REACTOR.getId(), ChemReactorRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation(ChemReactorAddon.MOD_ID, "chem_reactor");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new IRecipeCategory[]{
                new ChemReactorRecipeCategory(registration.getJeiHelpers().getGuiHelper(),
                        new ItemStack(ChemReactorBlocks.CHEMICAL_REACTOR.get()), CHEM_REACTOR_TYPE)
        });
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeManager recipeManager = getRecipeManager();
        if (recipeManager != null) {
            registration.addRecipes(CHEM_REACTOR_TYPE, recipeManager.getAllRecipesFor(ChemReactorRecipeTypes.CHEM_REACTOR.get()));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ChemReactorBlocks.CHEMICAL_REACTOR.get()), new RecipeType[]{CHEM_REACTOR_TYPE});
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Клик по индикатору реакции на настоящем экране — открывает список рецептов в JEI,
        // как у всех родных машин Thermal (см. TExpJeiPlugin.registerGuiHandlers()).
        registration.addRecipeClickArea(ChemReactorScreen.class,
                ChemReactorScreen.PROGRESS_X, ChemReactorScreen.PROGRESS_Y,
                ChemReactorScreen.SPRITE_PROGRESS_W, ChemReactorScreen.SPRITE_PROGRESS_H,
                new RecipeType[]{CHEM_REACTOR_TYPE});
    }

    private static RecipeManager getRecipeManager() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null ? level.getRecipeManager() : null;
    }
}
