package com.sazdan.chemreactor.init;

import com.sazdan.chemreactor.ChemReactorAddon;
import com.sazdan.chemreactor.block.ChemReactorBlock;
import com.sazdan.chemreactor.block.entity.ChemReactorTile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// DeferredRegister — это "отложенный регистратор". Вместо того чтобы регистрировать
// блок напрямую (что небезопасно с точки зрения порядка загрузки модов), мы
// складываем сюда "обещание создать блок", а реальная регистрация происходит
// автоматически, когда Forge поднимает соответствующее событие (RegistryEvent).
//
// Это стандартный паттерн для всех современных Forge-модов (1.16+).
public class ChemReactorBlocks {

    // Создаём DeferredRegister специально для блоков (ForgeRegistries.BLOCKS),
    // привязанный к нашему modId — это значит, что все registry name внутри
    // будут автоматически получать namespace "thermal_addon_chemreactor:..."
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ChemReactorAddon.MOD_ID);

    // Регистрируем сам блок реактора.
    // .register("chemical_reactor", ...) — здесь "chemical_reactor" это registry name,
    // итоговый ResourceLocation будет: thermal_addon_chemreactor:chemical_reactor
    // (именно так, как мы и договаривались в спецификации).
    //
    // ChemReactorBlock теперь наследуется от TileBlockActive4Way (см. класс) и требует
    // Properties/tileClass/blockEntityType в конструкторе — тот же вызов, что и у реальных
    // машин Thermal в TExpBlocks.register(). ChemReactorBlockEntities.CHEMICAL_REACTOR::get —
    // ссылка на метод, а не сам RegistryObject: оба DeferredRegister взаимно ссылаются друг
    // на друга (блоку нужен BlockEntityType, BlockEntityType.Builder.of(...) нужен блок для
    // списка validBlocks) — это безопасно, т.к. обе стороны обёрнуты в ленивые лямбды/суплаеры
    // и разрешаются только во время самого события регистрации, не при загрузке класса.
    public static final RegistryObject<Block> CHEMICAL_REACTOR =
            BLOCKS.register("chemical_reactor", () -> new ChemReactorBlock(
                    Properties.of(Material.METAL, MaterialColor.METAL)
                            .strength(3.5f, 8.0f)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops(),
                    ChemReactorTile.class,
                    ChemReactorBlockEntities.CHEMICAL_REACTOR::get
            ));
}
