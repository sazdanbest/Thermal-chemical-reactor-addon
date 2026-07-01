package com.sazdan.chemreactor.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import cofh.core.client.gui.ContainerScreenCoFH;
import cofh.core.client.gui.element.panel.AugmentPanel;
import cofh.core.client.gui.element.panel.ConfigPanel;
import cofh.core.client.gui.element.panel.RSControlPanel;
import cofh.core.client.gui.element.panel.SecurityPanel;
import cofh.core.util.helpers.GuiHelper;
import cofh.lib.util.helpers.SecurityHelper;
import cofh.thermal.core.client.gui.ThermalGuiHelper;
import com.sazdan.chemreactor.block.entity.ChemReactorTile;
import com.sazdan.chemreactor.inventory.ChemReactorContainer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

// ChemReactorScreen — рисует окно реактора. Фон — chemical_reactor.png (свой, с уже
// встроенными инвентарём игрока и charge-слотом). Рамки предметных слотов и индикатор
// прогресса — общие спрайты Thermal из jei_handler.png (256x256), тот же файл, что Thermal
// использует для отображения в JEI. Баки — уже НЕ самодельная заливка через blit()+fill(),
// а настоящие cofh.core.client.gui.element.ElementFluidStorage (см. init()) — дают бесплатно
// реальную текстуру жидкости (не плоский цвет), деления-рисочки поверх (overlay-текстура) и
// тултип с названием/объёмом. Координаты — по разметке автора.
//
// Наследуемся от cofh.core.client.gui.ContainerScreenCoFH (а не голого AbstractContainerScreen,
// как раньше) — ради их системы выезжающих боковых панелей (Panel) и системы "элементов"
// (Element), которой пользуется в том числе ElementFluidStorage. Собственный фон/предметные
// слоты/прогресс по-прежнему рисуются вручную через blit() в renderBg() —
// ContainerScreenCoFH.renderBg() не вызываем (она рисует ЕЁ СОБСТВЕННЫЙ this.texture, который
// мы не используем).
public class ChemReactorScreen extends ContainerScreenCoFH<ChemReactorContainer> {

    // Координаты и спрайты ниже — public, т.к. на них же (без дублирования магических чисел)
    // ссылается ChemReactorRecipeCategory (см. client/compat/jei/) при отрисовке рецепта в JEI —
    // страница JEI переиспользует ровно ту же раскладку и тот же файл jei_handler.png, что и
    // настоящий экран машины.
    public static final ResourceLocation TEXTURE_BACKGROUND =
            new ResourceLocation("thermal_addon_chemreactor", "textures/gui/container/chemical_reactor.png");
    public static final ResourceLocation TEXTURE_WIDGETS =
            new ResourceLocation("thermal", "textures/gui/jei_handler.png");

    // Спрайты внутри jei_handler.png.
    public static final int SPRITE_SLOT_U = 0, SPRITE_SLOT_V = 96, SPRITE_SLOT_SIZE = 18;
    public static final int SPRITE_PROGRESS_U_OFF = 224, SPRITE_PROGRESS_U_ON = 240;
    public static final int SPRITE_PROGRESS_V = 16, SPRITE_PROGRESS_W = 16, SPRITE_PROGRESS_H = 17;

    // Позиции на экране (относительно leftPos/topPos) — см. разметку в чате.
    public static final int TANK_INPUT_0_X = 37, TANK_INPUT_1_X = 57, TANK_Y = 14;
    public static final int TANK_OUTPUT_X = 140, TANK_OUTPUT_Y = 21;
    public static final int TANK_SMALL_W = 18, TANK_SMALL_H = 34;
    public static final int TANK_MEDIUM_W = 18, TANK_MEDIUM_H = 42;

    public static final int SLOT_INPUT_0_X = 37, SLOT_INPUT_0_Y = 52;
    public static final int SLOT_INPUT_1_X = 57, SLOT_INPUT_1_Y = 52;
    public static final int SLOT_OUTPUT_0_X = 118, SLOT_OUTPUT_0_Y = 21;
    public static final int SLOT_OUTPUT_1_X = 118, SLOT_OUTPUT_1_Y = 45;

    public static final int PROGRESS_X = 89, PROGRESS_Y = 35;

    // (8,8) — та же позиция RF-полоски, что и у настоящих машин Thermal
    // (cofh.thermal.lib.client.gui.MachineScreen.init(): GuiHelper.createDefaultEnergyStorage(this, 8, 8, ...)).
    // Иконка charge-слота на фоне нарисована ниже (примерно x9-20,y55-66), не пересекается.
    private static final int ENERGY_BAR_X = 8, ENERGY_BAR_Y = 8;

    private final ChemReactorTile tile;

    public ChemReactorScreen(ChemReactorContainer menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.tile = menu.tile;
        this.name = "chemical_reactor";
        // Литеральный (не lang-key!) текст для InfoPanel собирается из ключей
        // info.thermal_addon_chemreactor.chemical_reactor.0/.1/.2 (en_us/ru_ru) —
        // GuiHelper.generatePanelInfo() читает их по порядку, пока ключ существует.
        this.info = GuiHelper.generatePanelInfo("info.thermal_addon_chemreactor.chemical_reactor");
    }

    @Override
    public void init() {
        super.init();
        // Полный набор боковых панелей, как у настоящих машин Thermal (AugmentableTileScreen +
        // MachineScreen вместе) — Security/Augment/RSControl слева-справа по умолчанию CoFH сама
        // решает (defaultSide у каждого класса), Config и Energy — то же самое, что делает
        // MachineScreen.init() у родных машин. this.info уже задан в конструкторе, поэтому
        // ContainerScreenCoFH.init() (вызванный через super.init() чуть выше) уже сам добавил
        // InfoPanel — вручную его создавать не нужно.
        this.addPanel(new SecurityPanel(this, this.tile, SecurityHelper.getID(this.player)));
        this.addPanel(new AugmentPanel(this, this.menu::getNumAugmentSlots, this.menu.getAugmentSlots()));
        this.addPanel(new RSControlPanel(this, this.tile));
        this.addPanel(new ConfigPanel(this, this.tile, this.tile, this.tile::getFacing)
                .addConditionals(ThermalGuiHelper.createDefaultMachineConfigs(this, this.name, this.tile)));

        if (this.tile.getEnergyStorage() != null && this.tile.getEnergyStorage().getMaxEnergyStored() > 0) {
            this.addPanel(ThermalGuiHelper.createDefaultEnergyUserPanel(this, this.tile));
            this.addElement(GuiHelper.setClearable(
                    GuiHelper.createDefaultEnergyStorage(this, ENERGY_BAR_X, ENERGY_BAR_Y, this.tile.getEnergyStorage()),
                    this.tile, 0));
        }

        // Баки — настоящие ElementFluidStorage (реальная текстура жидкости + деления-рисочки +
        // тултип с названием/объёмом, всё бесплатно). Small = 18x34 (наши входные баки), Medium =
        // 18x42 (выходной) — размеры совпадают с теми, что мы и так использовали для рамок из
        // jei_handler.png, поэтому переверстывать раскладку экрана не пришлось. *InputFluidStorage/
        // *OutputFluidStorage дополнительно рисуют подсветку, если соответствующая грань блока
        // настроена на вход/выход в ConfigPanel (reconfig::hasInputSide/hasOutputSide).
        this.addElement(GuiHelper.setClearable(
                GuiHelper.createSmallInputFluidStorage(this, TANK_INPUT_0_X, TANK_Y, this.tile.getTank(0), this.tile), this.tile, 0));
        this.addElement(GuiHelper.setClearable(
                GuiHelper.createSmallInputFluidStorage(this, TANK_INPUT_1_X, TANK_Y, this.tile.getTank(1), this.tile), this.tile, 1));
        this.addElement(GuiHelper.setClearable(
                GuiHelper.createMediumOutputFluidStorage(this, TANK_OUTPUT_X, TANK_OUTPUT_Y, this.tile.getTank(2), this.tile), this.tile, 2));
    }

    @Override
    protected void renderBg(PoseStack poseStack, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Фон панели (176x166 в холсте 256x256) — инвентарь игрока и charge-слот уже на нём.
        RenderSystem.setShaderTexture(0, TEXTURE_BACKGROUND);
        this.blit(poseStack, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // Рамки предметных слотов — общие спрайты Thermal (баки теперь рисует ElementFluidStorage
        // через drawElements() ниже, у него своя рамка/фон в текстуре storage_fluid_*.png).
        RenderSystem.setShaderTexture(0, TEXTURE_WIDGETS);
        blitSlotFrame(poseStack, SLOT_INPUT_0_X, SLOT_INPUT_0_Y);
        blitSlotFrame(poseStack, SLOT_INPUT_1_X, SLOT_INPUT_1_Y);
        blitSlotFrame(poseStack, SLOT_OUTPUT_0_X, SLOT_OUTPUT_0_Y);
        blitSlotFrame(poseStack, SLOT_OUTPUT_1_X, SLOT_OUTPUT_1_Y);
        // Charge-слот (индекс 4 в ChemReactorContainer, позиция (8,53)) — БЕЗ своей рамки: на
        // фоне chemical_reactor.png там уже нарисована готовая иконка/рамка (проверено разбором
        // PNG по пикселям: иконка занимает примерно x9-20,y55-66 — точно под этим слотом), рисовать
        // поверх неё ещё и jei_handler-рамку не нужно (так и у настоящего Brewer — там тоже свой
        // фон, а не отдельный blit под charge-слот).

        renderProgress(poseStack);

        // Все боковые панели (Security/Augment/RSControl/Config/Energy/Info) и элементы
        // (RF-полоска, 3 ElementFluidStorage) — добавлены в init(). Родительский
        // ContainerScreenCoFH.renderBg() делает то же самое после отрисовки СВОЕГО фона — мы его
        // не вызываем (свой фон рисуем сами выше), поэтому дублируем только этот хвост.
        poseStack.pushPose();
        poseStack.translate(this.leftPos, this.topPos, 0.0D);
        this.drawPanels(poseStack, false);
        this.drawElements(poseStack, false);
        poseStack.popPose();
    }

    private void blitSlotFrame(PoseStack poseStack, int x, int y) {
        this.blit(poseStack, this.leftPos + x, this.topPos + y, SPRITE_SLOT_U, SPRITE_SLOT_V, SPRITE_SLOT_SIZE, SPRITE_SLOT_SIZE);
    }

    // Индикатор реакции: серый "выкл" спрайт всегда, поверх — синий "вкл" спрайт, обрезанный
    // по ширине пропорционально прогрессу (тот же приём, что furnace-style прогресс-бары —
    // раскрывается слева направо). getScaledProgress(scale) уже даёт готовое значение 0..scale.
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
}
