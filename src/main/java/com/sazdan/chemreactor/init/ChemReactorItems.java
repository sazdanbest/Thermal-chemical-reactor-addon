package com.sazdan.chemreactor.init;

import com.sazdan.chemreactor.ChemReactorAddon;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Аналогично ChemReactorBlocks, но для предметов.
// Важный момент: каждый блок, который игрок должен иметь возможность
// положить в инвентарь и поставить в мире, требует ОТДЕЛЬНОЙ регистрации
// как Item (через BlockItem-обёртку) — простой регистрации блока недостаточно.
public class ChemReactorItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ChemReactorAddon.MOD_ID);

    // BlockItem — это "предмет-обёртка" над блоком: именно он лежит в инвентаре,
    // ставится по правому клику и т.д. Используем то же registry name
    // "chemical_reactor", что и у блока — это стандартная практика Minecraft/Forge,
    // итоговый id: thermal_addon_chemreactor:chemical_reactor (для Item).
    public static final RegistryObject<Item> CHEMICAL_REACTOR =
            ITEMS.register("chemical_reactor", () -> new BlockItem(
                    ChemReactorBlocks.CHEMICAL_REACTOR.get(),
                    new Item.Properties().tab(CreativeModeTab.TAB_MISC) // временно кладём во вкладку "Разное"
                                                                          // позже можно перенести в свою/Thermal-вкладку
            ));
}
