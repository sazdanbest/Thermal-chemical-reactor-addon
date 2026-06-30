package com.sazdan.chemreactor.block;

import com.sazdan.chemreactor.block.entity.ChemReactorTile;
import com.sazdan.chemreactor.init.ChemReactorBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

// ChemReactorBlock — физическая оболочка блока в мире.
//
// На этом шаге (3) блок получает две новые способности по сравнению с шагом 2:
//   1. EntityBlock — связывает блок с ChemReactorTile (без этого Tile не создастся
//      при установке блока, и весь функционал из MachineBlockEntity не заработает).
//   2. FACING (на основе BlockStateProperties.HORIZONTAL_FACING) — блок поворачивается
//      фронтом к игроку при установке, как и положено машине Thermal.
public class ChemReactorBlock extends Block implements EntityBlock {

    // DirectionProperty ограниченный 4 горизонтальными направлениями (север/юг/восток/запад).
    // Машины Thermal не смотрят вверх/вниз — это соответствует тому, что мы видели
    // в Reconfigurable4WayBlockEntity (FACING_HORIZONTAL), от которого наследуется наш Tile.
    public static final DirectionProperty FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

    public ChemReactorBlock() {
        super(
                BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL)
                        .strength(3.5f, 8.0f)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
        );
        // Задаём состояние по умолчанию — фронт смотрит на север, пока игрок не установит блок.
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    // Регистрируем свойство FACING в "таблице состояний" блока — без этого
    // вызова Forge не будет знать, что у блока вообще есть направление,
    // и blockstate-файл не сможет на него ссылаться через variants.
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    // Вызывается при установке блока игроком. Здесь мы вычисляем направление:
    // getHorizontalDirection() возвращает направление, противоположное взгляду игрока —
    // то есть фронт блока "смотрит на" игрока, который его ставит.
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    // --- Связь блока с BlockEntity ---
    // Это и есть точка интеграции EntityBlock: Forge вызывает этот метод
    // при установке блока, чтобы создать соответствующий Tile.
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChemReactorTile(pos, state);
    }

    // Тикер — это то, что заставляет tickServer() в ChemReactorTile/MachineBlockEntity
    // вызываться каждый игровой тик. Без этого метода машина будет создана,
    // но никогда не будет "тикать" — то есть никогда не начнёт обработку.
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Тикаем только на сервере (логика обработки не должна выполняться на клиенте).
        if (level.isClientSide) {
            return null;
        }
        return type == ChemReactorBlockEntities.CHEMICAL_REACTOR.get()
                ? (lvl, pos, st, blockEntity) -> ((ChemReactorTile) blockEntity).tickServer()
                : null;
    }

    // Открытие интерфейса по правому клику. ChemReactorTile реализует MenuProvider
    // (через AugmentableBlockEntity) и уже умеет создавать ChemReactorContainer
    // в createMenu() — здесь только просим Forge открыть это меню у конкретного игрока.
    // NetworkHooks.openScreen(player, menuProvider, pos) сам допишет BlockPos в пакет,
    // который ChemReactorMenus читает на клиенте через data.readBlockPos().
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof MenuProvider menuProvider) {
                NetworkHooks.openScreen((ServerPlayer) player, menuProvider, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

