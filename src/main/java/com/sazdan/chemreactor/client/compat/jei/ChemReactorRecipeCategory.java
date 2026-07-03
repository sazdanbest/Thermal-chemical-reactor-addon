package com.sazdan.chemreactor.client.compat.jei;

import cofh.core.util.helpers.ItemHelper;
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
import mezz.jei.api.gui.ingredient.IRecipeSlotTooltipCallback;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
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
// TEXTURE_WIDGETS в ChemReactorScreen) для рамок предметных слотов; полосу прогресса — тот же спрайт,
// что и у настоящего экрана (SPRITE_PROGRESS_*), для визуальной согласованности между GUI и JEI.
//
// Рамки баков — НЕ из Drawables (там общий ассет 18x62 у всех размеров, слишком высокий, чтобы под
// баком поместился ещё и предметный слот), а те же файлы, что реально рисует ChemReactorScreen через
// GuiHelper.createSmallInputFluidStorage()/createMediumOutputFluidStorage() — cofh_core:textures/
// gui/elements/storage_fluid_{small,medium}.png (18x34 / 18x42, см. CLAUDE.md). Раскладка ниже
// повторяет основной GUI: бак сверху, под ним свой предметный слот (для обоих входов).
public class ChemReactorRecipeCategory extends ThermalRecipeCategory<ChemReactorRecipe> {

    private static final ResourceLocation TEXTURE_TANK_SMALL =
            new ResourceLocation("cofh_core", "textures/gui/elements/storage_fluid_small.png");
    private static final ResourceLocation TEXTURE_TANK_MEDIUM =
            new ResourceLocation("cofh_core", "textures/gui/elements/storage_fluid_medium.png");
    private static final int TANK_TEXTURE_W = 32, TANK_TEXTURE_H = 64;
    private static final int TANK_SMALL_W = 18, TANK_SMALL_H = 34;
    private static final int TANK_MEDIUM_W = 18, TANK_MEDIUM_H = 42;

    // Раскладка на канве категории (локальные координаты, не связаны с координатами настоящего
    // экрана — там другая система координат и другой набор виджетов). Бак жидкости сверху, свой
    // предметный слот строго под ним — как в ChemReactorScreen.
    private static final int TANK_IN_0_X = 26, TANK_IN_1_X = 48, TANK_IN_Y = 4;
    private static final int ITEM_IN_0_X = 26, ITEM_IN_0_Y = 42;
    private static final int ITEM_IN_1_X = 48, ITEM_IN_1_Y = 42;
    private static final int PROGRESS_X = 74, PROGRESS_Y = 26;
    private static final int ITEM_OUT_0_X = 100, ITEM_OUT_0_Y = 8;
    private static final int ITEM_OUT_1_X = 100, ITEM_OUT_1_Y = 32;
    private static final int TANK_OUT_X = 126, TANK_OUT_Y = 8;

    private static final int CANVAS_W = 150, CANVAS_H = 70;

    private final IDrawableStatic itemSlotFrame;
    private final IDrawableStatic tankFrameInput;
    private final IDrawableStatic tankFrameOutput;
    private final IDrawableStatic tankOverlayInput;
    private final IDrawableStatic tankOverlayOutput;
    private final IDrawableAnimated progressOn;
    private final IDrawableStatic progressOff;

    public ChemReactorRecipeCategory(IGuiHelper guiHelper, ItemStack icon, RecipeType<ChemReactorRecipe> type) {
        super(guiHelper, icon, type);
        this.background = guiHelper.createBlankDrawable(CANVAS_W, CANVAS_H);
        this.name = icon.getHoverName();

        Drawables drawables = Drawables.getDrawables(guiHelper);
        this.itemSlotFrame = drawables.getSlot(Drawables.SLOT);
        this.tankFrameInput = guiHelper.drawableBuilder(TEXTURE_TANK_SMALL, 0, 0, TANK_SMALL_W, TANK_SMALL_H)
                .setTextureSize(TANK_TEXTURE_W, TANK_TEXTURE_H).build();
        this.tankFrameOutput = guiHelper.drawableBuilder(TEXTURE_TANK_MEDIUM, 0, 0, TANK_MEDIUM_W, TANK_MEDIUM_H)
                .setTextureSize(TANK_TEXTURE_W, TANK_TEXTURE_H).build();
        this.tankOverlayInput = drawables.getTankOverlay(Drawables.TANK_SMALL);
        this.tankOverlayOutput = drawables.getTankOverlay(Drawables.TANK_MEDIUM);

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

        addFluidSlot(builder, RecipeIngredientRole.INPUT, TANK_IN_0_X, TANK_IN_Y,
                inputFluids.isEmpty() ? Collections.emptyList() : List.of(inputFluids.get(0).getFluids()),
                4000, this.tankOverlayInput, 32);
        addFluidSlot(builder, RecipeIngredientRole.INPUT, TANK_IN_1_X, TANK_IN_Y,
                inputFluids.size() < 2 ? Collections.emptyList() : List.of(inputFluids.get(1).getFluids()),
                4000, this.tankOverlayInput, 32);

        // Выходные предметы группируются по РАЗНЫМ предметам — так же, как их складывает
        // сама машина (MachineBlockEntity.resolveOutputs() докладывает совпадающий предмет
        // в тот же слот). Несколько записей одного предмета с разными шансами (например
        // "1 + 50% + 25%" — случайно 1-3 шт.) показываются ОДНИМ слотом с объединённым
        // тултипом по всем шансам, а не дублируются по двум слотам. Групп не больше
        // MAX_OUTPUT_ITEMS=2 (это гарантирует валидация в ChemReactorRecipeManager).
        List<List<Integer>> outputGroups = new ArrayList<>();
        for (int i = 0; i < outputItems.size(); ++i) {
            if (outputItems.get(i).isEmpty()) {
                continue;
            }
            List<Integer> group = null;
            for (List<Integer> g : outputGroups) {
                if (ItemHelper.itemsEqualWithTags(outputItems.get(g.get(0)), outputItems.get(i))) {
                    group = g;
                    break;
                }
            }
            if (group == null) {
                group = new ArrayList<>();
                outputGroups.add(group);
            }
            group.add(i);
        }

        int[] outX = {ITEM_OUT_0_X, ITEM_OUT_1_X};
        int[] outY = {ITEM_OUT_0_Y, ITEM_OUT_1_Y};
        for (int s = 0; s < 2; ++s) {
            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, outX[s] + 1, outY[s] + 1);
            if (s < outputGroups.size()) {
                List<Integer> group = outputGroups.get(s);
                slot.addItemStack(outputItems.get(group.get(0)));
                List<Float> groupChances = new ArrayList<>(group.size());
                for (int idx : group) {
                    groupChances.add(outputChances.get(idx));
                }
                slot.addTooltipCallback(chanceTooltip(groupChances));
            }
        }

        addFluidSlot(builder, RecipeIngredientRole.OUTPUT, TANK_OUT_X, TANK_OUT_Y,
                outputFluids.isEmpty() ? Collections.emptyList() : List.of(outputFluids.get(0)),
                8000, this.tankOverlayOutput, 40);
    }

    // Тултип для выходного слота. Для одной записи — штатный тултип Thermal
    // (TCoreJeiPlugin.defaultOutputTooltip). Для нескольких записей одного предмета
    // (случайное количество) — тултип каждого шанса подряд, чтобы игрок видел все
    // строки "Шанс: X%" сразу (guaranteed-запись с шансом -1/100% свою строку не рисует).
    private static IRecipeSlotTooltipCallback chanceTooltip(List<Float> chances) {
        if (chances.size() == 1) {
            return TCoreJeiPlugin.defaultOutputTooltip(chances.get(0));
        }
        List<IRecipeSlotTooltipCallback> callbacks = new ArrayList<>(chances.size());
        for (float chance : chances) {
            callbacks.add(TCoreJeiPlugin.defaultOutputTooltip(chance));
        }
        return (view, tooltip) -> {
            for (IRecipeSlotTooltipCallback cb : callbacks) {
                cb.onTooltip(view, tooltip);
            }
        };
    }

    private void addFluidSlot(IRecipeLayoutBuilder builder, RecipeIngredientRole role, int x, int y,
                               List<FluidStack> fluids, int capacity, IDrawableStatic overlay, int renderHeight) {
        // ВАЖНО: ёмкость для рендера — РЕАЛЬНАЯ ёмкость бака (4000/8000 mB), НЕ через
        // TCoreJeiPlugin.tankSize(capacity). tankSize() при включённом клиент-конфиге Thermal
        // ThermalClientConfig.jeiBucketTanks возвращает 1000 (одно ведро) вместо настоящей
        // ёмкости — тогда JEI считает заполнение относительно 1000 mB, и, например, 500 mB
        // рисуется как половина бака, хотя в самой машине (ёмкость 4000) это 1/8. Разработчик
        // просил, чтобы визуальное заполнение в JEI совпадало с машиной, поэтому берём реальную
        // ёмкость напрямую — 500/4000 = 1/8, как в GUI. (Тултип по-прежнему показывает точный
        // объём "X / Y mB" через defaultFluidTooltip, но полоса теперь пропорциональна машине.)
        ((IRecipeSlotBuilder) builder.addSlot(role, x + 1, y + 1).addIngredients(ForgeTypes.FLUID_STACK, fluids))
                .setFluidRenderer(capacity, false, 16, renderHeight)
                .setOverlay(overlay, 0, 0)
                .addTooltipCallback(TCoreJeiPlugin.defaultFluidTooltip());
    }

    @Override
    public void draw(ChemReactorRecipe recipe, IRecipeSlotsView recipeSlotsView, PoseStack poseStack, double mouseX, double mouseY) {
        super.draw(recipe, recipeSlotsView, poseStack, mouseX, mouseY);

        this.itemSlotFrame.draw(poseStack, ITEM_IN_0_X, ITEM_IN_0_Y);
        this.itemSlotFrame.draw(poseStack, ITEM_IN_1_X, ITEM_IN_1_Y);
        this.itemSlotFrame.draw(poseStack, ITEM_OUT_0_X, ITEM_OUT_0_Y);
        this.itemSlotFrame.draw(poseStack, ITEM_OUT_1_X, ITEM_OUT_1_Y);

        this.tankFrameInput.draw(poseStack, TANK_IN_0_X, TANK_IN_Y);
        this.tankFrameInput.draw(poseStack, TANK_IN_1_X, TANK_IN_Y);
        this.tankFrameOutput.draw(poseStack, TANK_OUT_X, TANK_OUT_Y);

        this.progressOff.draw(poseStack, PROGRESS_X, PROGRESS_Y);
        this.progressOn.draw(poseStack, PROGRESS_X, PROGRESS_Y);
    }
}
