package com.sazdan.chemreactor.inventory;

import cofh.core.inventory.container.TileCoFHContainer;
import cofh.lib.inventory.container.slot.SlotCoFH;
import cofh.lib.inventory.wrapper.InvWrapperCoFH;
import com.sazdan.chemreactor.block.entity.ChemReactorTile;
import com.sazdan.chemreactor.init.ChemReactorMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// ChemReactorContainer — связывает инвентарь тайла с окном на экране игрока.
// Наследуемся от TileCoFHContainer (CoFH) — он уже даёт бесплатно:
//   - quickMoveStack (шифт-клик между слотами тайла и инвентарём игрока)
//   - stillValid (закрытие окна, если игрок отошёл/блок сломан)
// Поэтому самим писать эту логику с нуля не нужно — как и в MachineBrewerContainer.
public class ChemReactorContainer extends TileCoFHContainer {

    public final ChemReactorTile tile;

    public ChemReactorContainer(int windowId, Level level, BlockPos pos, Inventory inventory, Player player) {
        super(ChemReactorMenus.CHEM_REACTOR.get(), windowId, level, pos, inventory, player);
        this.tile = (ChemReactorTile) level.getBlockEntity(pos);

        // InvWrapperCoFH оборачивает SimpleItemInv тайла в обычный vanilla Container,
        // чтобы с ним мог работать стандартный (и CoFH-шный) механизм слотов.
        InvWrapperCoFH tileInv = new InvWrapperCoFH(this.tile.getItemInv());

        // Индексы 0-1 — входные предметные слоты, 2-3 — выходные
        // (именно в этом порядке они были добавлены в ChemReactorTile.inventory).
        // Координаты = позиция рамки слота на экране (см. ChemReactorScreen) + 1px,
        // т.к. сам предмет рисуется на 1px внутрь рамки (как в vanilla/Thermal).
        this.addSlot(new SlotCoFH(tileInv, 0, 38, 53));
        this.addSlot(new SlotCoFH(tileInv, 1, 58, 53));
        this.addSlot(new SlotCoFH(tileInv, 2, 119, 22));
        this.addSlot(new SlotCoFH(tileInv, 3, 119, 46));

        // Индекс 4 — charge-слот (добавлен в ChemReactorTile сразу после 4 предметных слотов,
        // перед баками). Позиция (8,53) — подогнана разработчиком по месту вручную (на 1px левее/
        // выше, чем изначально) под уже нарисованную иконку на фоне chemical_reactor.png.
        this.addSlot(new SlotCoFH(tileInv, 4, 8, 53));

        // Аугмент-слоты — индексы 5..(5+augSize()-1): 4 предметных слота (0-3) + charge-слот (4)
        // добавлены раньше, аугменты по-прежнему последние (см. addAugmentSlots() в ChemReactorTile).
        // bindAugmentSlots() создаёт SlotCoFH на (0,0) — координаты на экране им проставляет
        // AugmentPanel/ElementAugmentSlots в ChemReactorScreen (та же выезжающая боковая панель,
        // что и у настоящих машин Thermal), поэтому здесь ничего вручную не позиционируем.
        this.bindAugmentSlots(tileInv, 5, this.tile.augSize());

        this.bindPlayerInventory(inventory);
    }
}
