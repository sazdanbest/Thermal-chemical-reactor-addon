package com.sazdan.chemreactor.init;

import com.sazdan.chemreactor.ChemReactorAddon;
import com.sazdan.chemreactor.block.entity.ChemReactorTile;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Регистрация BlockEntityType — это "паспорт типа" нашего Tile, отдельный
// от регистрации самого блока. Без этой регистрации Forge не будет знать,
// как создавать/сохранять/загружать ChemReactorTile при чтении мира.
public class ChemReactorBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ChemReactorAddon.MOD_ID);

    // BlockEntityType.Builder.of(factory, validBlocks...) — говорит Forge:
    //   "вот функция создания тайла" + "вот список блоков, для которых этот тайл валиден"
    // .build(null) — последний параметр это DataFixer (используется Mojang для миграции
    // старых сохранений между версиями игры), для модов почти всегда оставляют null.
    //
    // ChemReactorBlocks.CHEMICAL_REACTOR используется здесь как RegistryObject<Block>,
    // но т.к. на момент регистрации блок ещё не гарантированно создан, берём его
    // через .get() лениво — Forge сам разрешит порядок инициализации регистров.
    public static final RegistryObject<BlockEntityType<ChemReactorTile>> CHEMICAL_REACTOR =
            BLOCK_ENTITIES.register("chemical_reactor", () -> BlockEntityType.Builder.of(
                    ChemReactorTile::new,
                    ChemReactorBlocks.CHEMICAL_REACTOR.get()
            ).build(null));
}
