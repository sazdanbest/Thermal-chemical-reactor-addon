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
//
// Аугмент-слоты пока НЕ добавляем — ChemReactorTile их ещё не настраивает
// (addAugmentSlots не вызывается в конструкторе тайла), это отдельная задача на будущее.
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
        this.addSlot(new SlotCoFH(tileInv, 0, 53, 12));
        this.addSlot(new SlotCoFH(tileInv, 1, 53, 34));
        this.addSlot(new SlotCoFH(tileInv, 2, 116, 12));
        this.addSlot(new SlotCoFH(tileInv, 3, 116, 34));

        this.bindPlayerInventory(inventory);
    }
}
