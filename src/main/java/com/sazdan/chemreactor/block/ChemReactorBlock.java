package com.sazdan.chemreactor.block;

import cofh.core.block.TileBlockActive4Way;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Supplier;

// ChemReactorBlock — физическая оболочка блока в мире.
//
// Наследуется от cofh.core.block.TileBlockActive4Way — того же базового класса, что и
// РЕАЛЬНЫЕ машины Thermal (см. TExpBlocks.register(), где даже Brewer — это буквально
// "new TileBlockActive4Way(...)" без собственного подкласса). Раньше (шаги 1-3) этот класс
// наследовался от голого Block и переизобретал вручную FACING/createBlockStateDefinition/
// getStateForPlacement/newBlockEntity/getTicker/use() — весь этот код удалён, потому что
// TileBlockActive4Way (через TileBlock4Way → TileBlockCoFH) уже даёт бесплатно:
//   - свойства состояния FACING_HORIZONTAL (4 стороны) + ACTIVE (вкл/выкл текстура) —
//     то же самое, что мы регистрировали руками, теперь не нужно;
//   - newBlockEntity()/getTicker() через переданные tileClass/blockEntityType;
//   - use(): открытие GUI (то же самое, что мы писали руками), ПЛЮС то, чего у нас не было:
//     гаечный ключ (поворот/демонтаж), проверка Security-доступа, и — САМОЕ ВАЖНОЕ —
//     взаимодействие с ведром жидкости (TileCoFH.onActivatedDelegate() пробует влить/слить
//     через IFluidHandler-капабилити тайла ПЕРЕД открытием GUI; если жидкость не подходит —
//     просто открывается GUI, как обычный правый клик);
//   - onRemove(): вызывает tile.onReplaced(...), из-за чего при поломке блока выпадают
//     предметы/аугменты (AugmentableBlockEntity.onReplaced() уже это делает — просто раньше
//     этот метод никто не вызывал, т.к. голый Block.onRemove() ничего подобного не делает);
//   - neighborChanged(): пробрасывает сигнал редстоуна в tile.neighborChanged() → без этого
//     redstoneControl.power вообще не обновлялся после первой установки блока (сохранялось
//     значение на момент placement) — из-за этого режимы "Низкий"/"Высокий" у RSControlPanel
//     выглядели сломанными: power просто никогда не менялся.
// Плюс модель блока теперь через "parent": "thermal:block/machine_base" (см. models/block/) —
// у этого родителя "loader": "thermal:reconfigurable", который рисует поверх граней блока
// цветные иконки входа/выхода/обоих/недоступно из ConfigPanel — то, чего не было у
// "minecraft:block/orientable".
public class ChemReactorBlock extends TileBlockActive4Way {

    public ChemReactorBlock(BlockBehaviour.Properties properties, Class<?> tileClass, Supplier<BlockEntityType<?>> blockEntityType) {
        super(properties, tileClass, blockEntityType);
    }
}
