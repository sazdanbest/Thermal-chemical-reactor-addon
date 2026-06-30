package com.sazdan.chemreactor.client;

import com.mojang.blaze3d.vertex.PoseStack;
import cofh.lib.fluid.FluidStorageCoFH;
import com.sazdan.chemreactor.inventory.ChemReactorContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

// ChemReactorScreen — рисует окно реактора. Своей текстуры фона у нас пока нет
// (см. CLAUDE.md, раздел "Ресурсы и текстуры" — артов для GUI ещё не делали), поэтому
// фон/слоты/бак рисуем примитивами (fill) вместо blit() готовой картинки. Слоты с
// предметами и тултипы по ним рисует сама AbstractContainerScreen — переопределять
// для этого ничего не нужно, нужен только renderBg().
public class ChemReactorScreen extends AbstractContainerScreen<ChemReactorContainer> {

    // Координаты внутри окна (относительно leftPos/topPos) — см. ChemReactorContainer
    // для тех же координат предметных слотов, чтобы рамки совпадали со слотами.
    private static final int TANK_WIDTH = 14;
    private static final int TANK_HEIGHT = 47;
    private static final int TANK_INPUT_0_X = 8;
    private static final int TANK_INPUT_1_X = 26;
    private static final int TANK_OUTPUT_X = 150;
    private static final int TANK_Y = 12;

    private static final int PROGRESS_X = 80;
    private static final int PROGRESS_Y = 24;
    private static final int PROGRESS_WIDTH = 24;
    private static final int PROGRESS_HEIGHT = 10;

    public ChemReactorScreen(ChemReactorContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        // Фон окна — просто закрашенная панель с рамкой (заглушка вместо текстуры).
        this.fill(poseStack, this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFFC6C6C6);
        this.fill(poseStack, this.leftPos + 1, this.topPos + 1, this.leftPos + this.imageWidth - 1, this.topPos + this.imageHeight - 1, 0xFF8B8B8B);

        renderTank(poseStack, this.menu.tile.getTank(0), TANK_INPUT_0_X, TANK_Y);
        renderTank(poseStack, this.menu.tile.getTank(1), TANK_INPUT_1_X, TANK_Y);
        renderTank(poseStack, this.menu.tile.getTank(2), TANK_OUTPUT_X, TANK_Y);

        renderProgress(poseStack);
    }

    // Простой бак: рамка + заливка пропорционально заполнению, цвет — родной тинт жидкости
    // (через IClientFluidTypeExtensions, как это делает сам Forge в стандартных тултипах).
    private void renderTank(PoseStack poseStack, FluidStorageCoFH tank, int x, int y) {
        int left = this.leftPos + x;
        int top = this.topPos + y;
        this.fill(poseStack, left - 1, top - 1, left + TANK_WIDTH + 1, top + TANK_HEIGHT + 1, 0xFF373737);

        if (!tank.isEmpty() && tank.getCapacity() > 0) {
            FluidStack fluidStack = tank.getFluidStack();
            int filledHeight = Math.round((float) TANK_HEIGHT * tank.getAmount() / tank.getCapacity());
            int color = IClientFluidTypeExtensions.of(fluidStack.getFluid()).getTintColor(fluidStack);
            this.fill(poseStack, left, top + (TANK_HEIGHT - filledHeight), left + TANK_WIDTH, top + TANK_HEIGHT, color);
        }
    }

    // Прогресс цикла обработки — растущий прямоугольник вместо анимированной стрелки.
    // getScaledProgress(scale) уже даёт готовое масштабированное значение (0..scale).
    private void renderProgress(PoseStack poseStack) {
        int left = this.leftPos + PROGRESS_X;
        int top = this.topPos + PROGRESS_Y;
        this.fill(poseStack, left - 1, top - 1, left + PROGRESS_WIDTH + 1, top + PROGRESS_HEIGHT + 1, 0xFF373737);
        int filled = this.menu.tile.getScaledProgress(PROGRESS_WIDTH);
        if (filled > 0) {
            this.fill(poseStack, left, top, left + filled, top + PROGRESS_HEIGHT, 0xFFE0A030);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }
}
