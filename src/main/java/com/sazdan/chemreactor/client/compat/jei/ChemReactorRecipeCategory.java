package com.sazdan.chemreactor.client.compat.jei;

import cofh.lib.fluid.FluidIngredient;
import cofh.thermal.core.compat.jei.TCoreJeiPlugin;
import cofh.thermal.lib.compat.jei.Drawables;
import cofh.thermal.lib.compat.jei.ThermalRecipeCategory;
import com.sazdan.chemreactor.client.ChemReactorScreen;
import com.sazdan.chemreactor.recipe.ChemReactorRecipe;
import mezz.jei.api.forge.ForgeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.List;

// Как JEI показывает рецепт Chemical Reactor. По образцу PressRecipeCategory/BrewerRecipeCategory
// самого Thermal (cofh.thermal.expansion.compat.jei.machine, декомпилировано и проверено при
// разработке этого класса) — extends ThermalRecipeCategory даёт бесплатно RF-иконку с тултипом
// (см. draw()/getTooltipStrings() родителя, cofh.thermal.lib.compat.jei.ThermalRecipeCategory).
//
// Фон — пустая канва (а не кроп настоящего экрана): у родных машин Thermal фон категории — кроп их
// собственного MachineXScreen.TEXTURE, но их арт-стиль рисует рамки слотов прямо на этой текстуре;
// наш chemical_reactor.png так не делает (рамки/баки рисуются отдельно поверх, см. ChemReactorScreen),
// поэтому кроп дал бы пустые проплешины на месте баков/слотов. Вместо этого — как и сама Thermal,
// используем общие спрайты Drawables (thermal:textures/gui/jei_handler.png, тот же файл, что и
// TEXTURE_WIDGETS в ChemReactorScreen) для рамок слотов и баков; полосу прогресса — тот же спрайт,
// что и у настоящего экрана (SPRITE_PROGRESS_*), для визуальной согласованности между GUI и JEI.
public class ChemReactorRecipeCategory extends ThermalRecipeCategory<ChemReactorRecipe> {

    // Раскладка на канве категории (локальные координаты, не связаны с координатами настоящего
    // экрана — там другая система координат и другой набор виджетов).
    private static final int ITEM_IN_0_X = 4, ITEM_IN_0_Y = 8;
    private static final int ITEM_IN_1_X = 4, ITEM_IN_1_Y = 28;
    private static final int TANK_IN_0_X = 26, TANK_IN_1_X = 48, TANK_Y = 4;
    private static final int PROGRESS_X = 74, PROGRESS_Y = 26;
    private static final int ITEM_OUT_0_X = 100, ITEM_OUT_0_Y = 8;
    private static final int ITEM_OUT_1_X = 100, ITEM_OUT_1_Y = 28;
    private static final int TANK_OUT_X = 122;

    private static final int CANVAS_W = 150, CANVAS_H = 70;

    private final IDrawableStatic itemSlotFrame;
    private final IDrawableStatic tankFrame;
    private final IDrawableStatic tankOverlay;
    private final IDrawableAnimated progressOn;
    private final IDrawableStatic progressOff;

    public ChemReactorRecipeCategory(IGuiHelper guiHelper, ItemStack icon, RecipeType<ChemReactorRecipe> type) {
        super(guiHelper, icon, type);
        this.background = guiHelper.createBlankDrawable(CANVAS_W, CANVAS_H);
        this.name = icon.getHoverName();

        Drawables drawables = Drawables.getDrawables(guiHelper);
        this.itemSlotFrame = drawables.getSlot(Drawables.SLOT);
        this.tankFrame = drawables.getTank(Drawables.TANK_SMALL);
        this.tankOverlay = drawables.getTankOverlay(Drawables.TANK_SMALL);

        // Тот же спрайт "индикатора реакции", что и у настоящего экрана (см. ChemReactorScreen) —
        // серый "выкл" статично, синий "вкл" зацикленно проигрывается поверх (в реальной игре вместо
        // цикла используется getScaledProgress() от текущего тайла, но у JEI-страницы нет живого
        // тайла — здесь просто анимация "туда-обратно" с периодом 100 тиков, как у настоящего цикла
        // реактора, см. ChemReactorTile.getBaseProcessTick()/CLAUDE.md).
        this.progressOff = guiHelper.createDrawable(ChemReactorScreen.TEXTURE_WIDGETS,
                ChemReactorScreen.SPRITE_PROGRESS_U_OFF, ChemReactorScreen.SPRITE_PROGRESS_V,
                ChemReactorScreen.SPRITE_PROGRESS_W, ChemReactorScreen.SPRITE_PROGRESS_H);
        IDrawableStatic progressOnStatic = guiHelper.createDrawable(ChemReactorScreen.TEXTURE_WIDGETS,
                ChemReactorScreen.SPRITE_PROGRESS_U_ON, ChemReactorScreen.SPRITE_PROGRESS_V,
                ChemReactorScreen.SPRITE_PROGRESS_W, ChemReactorScreen.SPRITE_PROGRESS_H);
        this.progressOn = guiHelper.createAnimatedDrawable(progressOnStatic, 100, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<ChemReactorRecipe> getRecipeType() {
        return this.type;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ChemReactorRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> inputItems = recipe.getInputItems();
        List<FluidIngredient> inputFluids = recipe.getInputFluids();
        List<ItemStack> outputItems = recipe.getOutputItems();
        List<Float> outputChances = recipe.getOutputItemChances();
        List<FluidStack> outputFluids = recipe.getOutputFluids();

        builder.addSlot(RecipeIngredientRole.INPUT, ITEM_IN_0_X + 1, ITEM_IN_0_Y + 1)
                .addIngredients(inputItems.isEmpty() ? Ingredient.EMPTY : inputItems.get(0));
        builder.addSlot(RecipeIngredientRole.INPUT, ITEM_IN_1_X + 1, ITEM_IN_1_Y + 1)
                .addIngredients(inputItems.size() < 2 ? Ingredient.EMPTY : inputItems.get(1));

        addFluidSlot(builder, RecipeIngredientRole.INPUT, TANK_IN_0_X, TANK_Y,
                inputFluids.isEmpty() ? Collections.emptyList() : List.of(inputFluids.get(0).getFluids()), 4000);
        addFluidSlot(builder, RecipeIngredientRole.INPUT, TANK_IN_1_X, TANK_Y,
                inputFluids.size() < 2 ? Collections.emptyList() : List.of(inputFluids.get(1).getFluids()), 4000);

        IRecipeSlotBuilder itemOut0 = builder.addSlot(RecipeIngredientRole.OUTPUT, ITEM_OUT_0_X + 1, ITEM_OUT_0_Y + 1);
        if (!outputItems.isEmpty()) {
            itemOut0.addItemStack(outputItems.get(0))
                    .addTooltipCallback(TCoreJeiPlugin.defaultOutputTooltip(outputChances.get(0)));
        }
        IRecipeSlotBuilder itemOut1 = builder.addSlot(RecipeIngredientRole.OUTPUT, ITEM_OUT_1_X + 1, ITEM_OUT_1_Y + 1);
        if (outputItems.size() > 1) {
            itemOut1.addItemStack(outputItems.get(1))
                    .addTooltipCallback(TCoreJeiPlugin.defaultOutputTooltip(outputChances.get(1)));
        }

        addFluidSlot(builder, RecipeIngredientRole.OUTPUT, TANK_OUT_X, TANK_Y,
                outputFluids.isEmpty() ? Collections.emptyList() : List.of(outputFluids.get(0)), 8000);
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y,
                               List<FluidStack> fluids, int capacity) {
        ((IRecipeSlotBuilder) builder.addSlot(role, x + 1, y + 1).addIngredients(ForgeTypes.FLUID_STACK, fluids))
                .setFluidRenderer(TCoreJeiPlugin.tankSize(capacity), false, 16, 54)
                .setOverlay(this.tankOverlay, 0, 0)
                .addTooltipCallback(TCoreJeiPlugin.defaultFluidTooltip());
    }

    @Override
    public void draw(ChemReactorRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack poseStack, double mouseX, double mouseY) {
        super.draw(recipe, recipeSlotsView, poseStack, mouseX, mouseY);

        this.itemSlotFrame.draw(poseStack, ITEM_IN_0_X, ITEM_IN_0_Y);
        this.itemSlotFrame.draw(poseStack, ITEM_IN_1_X, ITEM_IN_1_Y);
        this.itemSlotFrame.draw(poseStack, ITEM_OUT_0_X, ITEM_OUT_0_Y);
        this.itemSlotFrame.draw(poseStack, ITEM_OUT_1_X, ITEM_OUT_1_Y);

        this.tankFrame.draw(poseStack, TANK_IN_0_X, TANK_Y);
        this.tankFrame.draw(poseStack, TANK_IN_1_X, TANK_Y);
        this.tankFrame.draw(poseStack, TANK_OUT_X, TANK_Y);

        this.progressOff.draw(poseStack, PROGRESS_X, PROGRESS_Y);
        this.progressOn.draw(poseStack, PROGRESS_X, PROGRESS_Y);
    }
}
