package com.sazdan.chemreactor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import cofh.lib.fluid.FluidStorageCoFH;
import com.sazdan.chemreactor.inventory.ChemReactorContainer;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

// ChemReactorScreen — рисует окно реактора. Фон — chemical_reactor.png (свой, с уже
// встроенными инвентарём игрока и charge-слотом). Рамки баков/предметных слотов и
// индикатор прогресса — общие спрайты Thermal из jei_handler.png (256x256), тот же
// файл, что Thermal использует для отображения в JEI. Координаты — по разметке автора.
public class ChemReactorScreen extends AbstractContainerScreen<ChemReactorContainer> {

    private static final ResourceLocation TEXTURE_BACKGROUND =
            new ResourceLocation("thermal_addon_chemreactor", "textures/gui/container/chemical_reactor.png");
    private static final ResourceLocation TEXTURE_WIDGETS =
            new ResourceLocation("thermal", "textures/gui/jei_handler.png");

    // Спрайты внутри jei_handler.png.
    private static final int SPRITE_TANK_IN_U = 128, SPRITE_TANK_IN_V = 192, SPRITE_TANK_IN_W = 18, SPRITE_TANK_IN_H = 34;
    private static final int SPRITE_TANK_OUT_U = 64, SPRITE_TANK_OUT_V = 192, SPRITE_TANK_OUT_W = 18, SPRITE_TANK_OUT_H = 42;
    private static final int SPRITE_SLOT_U = 0, SPRITE_SLOT_V = 96, SPRITE_SLOT_SIZE = 18;
    private static final int SPRITE_PROGRESS_U_OFF = 224, SPRITE_PROGRESS_U_ON = 240;
    private static final int SPRITE_PROGRESS_V = 16, SPRITE_PROGRESS_W = 16, SPRITE_PROGRESS_H = 17;

    // Позиции на экране (относительно leftPos/topPos) — см. разметку в чате.
    private static final int TANK_INPUT_0_X = 37, TANK_INPUT_1_X = 57, TANK_Y = 14;
    private static final int TANK_OUTPUT_X = 140, TANK_OUTPUT_Y = 21;

    private static final int SLOT_INPUT_0_X = 37, SLOT_INPUT_0_Y = 52;
    private static final int SLOT_INPUT_1_X = 57, SLOT_INPUT_1_Y = 52;
    private static final int SLOT_OUTPUT_0_X = 118, SLOT_OUTPUT_0_Y = 21;
    private static final int SLOT_OUTPUT_1_X = 118, SLOT_OUTPUT_1_Y = 45;

    private static final int PROGRESS_X = 89, PROGRESS_Y = 35;

    public ChemReactorScreen(ChemReactorContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Фон панели (176x166 в холсте 256x256) — инвентарь игрока и charge-слот уже на нём.
        RenderSystem.setShaderTexture(0, TEXTURE_BACKGROUND);
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Рамки баков и предметных слотов — общие спрайты Thermal.
        RenderSystem.setShaderTexture(0, TEXTURE_WIDGETS);
        this.blit(poseStack, this.leftPos + TANK_INPUT_0_X, this.topPos + TANK_Y,
                SPRITE_TANK_IN_U, SPRITE_TANK_IN_V, SPRITE_TANK_IN_W, SPRITE_TANK_IN_H);
        this.blit(poseStack, this.leftPos + TANK_INPUT_1_X, this.topPos + TANK_Y,
                SPRITE_TANK_IN_U, SPRITE_TANK_IN_V, SPRITE_TANK_IN_W, SPRITE_TANK_IN_H);
        this.blit(poseStack, this.leftPos + TANK_OUTPUT_X, this.topPos + TANK_OUTPUT_Y,
                SPRITE_TANK_OUT_U, SPRITE_TANK_OUT_V, SPRITE_TANK_OUT_W, SPRITE_TANK_OUT_H);

        blitSlotFrame(poseStack, SLOT_INPUT_0_X, SLOT_INPUT_0_Y);
        blitSlotFrame(poseStack, SLOT_INPUT_1_X, SLOT_INPUT_1_Y);
        blitSlotFrame(poseStack, SLOT_OUTPUT_0_X, SLOT_OUTPUT_0_Y);
        blitSlotFrame(poseStack, SLOT_OUTPUT_1_X, SLOT_OUTPUT_1_Y);

        // Заливка баков настоящим цветом жидкости — поверх рамки, с отступом 1px по краям,
        // чтобы сама рамка спрайта осталась видна (как и у предметных слотов).
        renderTankFluid(poseStack, this.menu.tile.getTank(0), TANK_INPUT_0_X, TANK_Y, SPRITE_TANK_IN_W, SPRITE_TANK_IN_H);
        renderTankFluid(poseStack, this.menu.tile.getTank(1), TANK_INPUT_1_X, TANK_Y, SPRITE_TANK_IN_W, SPRITE_TANK_IN_H);
        renderTankFluid(poseStack, this.menu.tile.getTank(2), TANK_OUTPUT_X, TANK_OUTPUT_Y, SPRITE_TANK_OUT_W, SPRITE_TANK_OUT_H);

        renderProgress(poseStack);
    }

    private void blitSlotFrame(PoseStack poseStack, int x, int y) {
        this.blit(poseStack, this.leftPos + x, this.topPos + y, SPRITE_SLOT_U, SPRITE_SLOT_V, SPRITE_SLOT_SIZE, SPRITE_SLOT_SIZE);
    }

    // Тинт берётся из IClientFluidTypeExtensions — так же, как это делает сам Forge в тултипах баков.
    // Заливка растёт снизу вверх пропорционально заполнению, с отступом 1px внутри рамки спрайта.
    private void renderTankFluid(PoseStack poseStack, FluidStorageCoFH tank, int x, int y, int spriteWidth, int spriteHeight) {
        if (tank.isEmpty() || tank.getCapacity() <= 0) {
            return;
        }
        int left = this.leftPos + x + 1;
        int top = this.topPos + y + 1;
        int innerWidth = spriteWidth - 2;
        int innerHeight = spriteHeight - 2;

        FluidStack fluidStack = tank.getFluidStack();
        int filledHeight = Math.round((float) innerHeight * tank.getAmount() / tank.getCapacity());
        int color = IClientFluidTypeExtensions.of(fluidStack.getFluid()).getTintColor(fluidStack);
        this.fill(poseStack, left, top + (innerHeight - filledHeight), left + innerWidth, top + innerHeight, color);
    }

    // Индикатор реакции: серый "выкл" спрайт всегда, поверх — синий "вкл" спрайт, обрезанный
    // по ширине пропорционально прогрессу (тот же приём, что furnace-style прогресс-бары —
    // раскрытие слева направо). getScaledProgress(scale) уже даёт готовое значение 0..scale.
    private void renderProgress(PoseStack poseStack) {
        RenderSystem.setShaderTexture(0, TEXTURE_WIDGETS);
        int left = this.leftPos + PROGRESS_X;
        int top = this.topPos + PROGRESS_Y;
        this.blit(poseStack, left, top, SPRITE_PROGRESS_U_OFF, SPRITE_PROGRESS_V, SPRITE_PROGRESS_W, SPRITE_PROGRESS_H);

        int filled = this.menu.tile.getScaledProgress(SPRITE_PROGRESS_W);
        if (filled > 0) {
            this.blit(poseStack, left, top, SPRITE_PROGRESS_U_ON, SPRITE_PROGRESS_V, filled, SPRITE_PROGRESS_H);
        }
    }

    @Override
    public void render(PoseStack poseStack, int mouseX, int mouseY, float partialTick) {
        super.render(poseStack, mouseX, mouseY, partialTick);
        this.renderTooltip(poseStack, mouseX, mouseY);
    }
}
