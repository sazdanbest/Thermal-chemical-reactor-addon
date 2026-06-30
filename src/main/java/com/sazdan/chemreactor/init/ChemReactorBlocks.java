package com.sazdan.chemreactor.init;

import com.sazdan.chemreactor.ChemReactorAddon;
import com.sazdan.chemreactor.block.ChemReactorBlock;
import net.minecraft.world.level.block.Block;
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
    public static final RegistryObject<Block> CHEMICAL_REACTOR =
            BLOCKS.register("chemical_reactor", ChemReactorBlock::new);
}
