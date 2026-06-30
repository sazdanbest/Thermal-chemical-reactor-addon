package com.sazdan.chemreactor;

import com.sazdan.chemreactor.init.ChemReactorBlockEntities;
import com.sazdan.chemreactor.init.ChemReactorBlocks;
import com.sazdan.chemreactor.init.ChemReactorItems;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

// Аннотация @Mod — это то, что Forge ищет при сканировании jar'а, чтобы понять
// "это входная точка мода с modId = thermal_addon_chemreactor".
// Значение здесь ДОЛЖНО совпадать с modId в mods.toml, иначе Forge не свяжет их.
@Mod(ChemReactorAddon.MOD_ID)
public class ChemReactorAddon {

    // Используем константу, чтобы не дублировать строку "thermal_addon_chemreactor"
    // по всему коду (она используется как namespace для ResourceLocation: например,
    // "thermal_addon_chemreactor:chemical_reactor").
    public static final String MOD_ID = "thermal_addon_chemreactor";

    public ChemReactorAddon() {
        // IEventBus — это "шина событий" мода, через которую Forge уведомляет нас
        // о разных этапах загрузки (регистрация блоков/предметов, общая настройка и т.д.)
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Регистрируем наши DeferredRegister'ы (см. ChemReactorBlocks / ChemReactorItems)
        // на шину событий мода. Именно это заставляет Forge реально создать
        // и зарегистрировать наш блок и предмет при старте игры.
        ChemReactorBlocks.BLOCKS.register(modEventBus);
        ChemReactorItems.ITEMS.register(modEventBus);
        ChemReactorBlockEntities.BLOCK_ENTITIES.register(modEventBus);
    }
}
