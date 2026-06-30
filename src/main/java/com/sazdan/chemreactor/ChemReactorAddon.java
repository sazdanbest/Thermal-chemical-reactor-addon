package com.sazdan.chemreactor;

import cofh.thermal.lib.common.ThermalRecipeManagers;
import com.sazdan.chemreactor.client.ChemReactorScreen;
import com.sazdan.chemreactor.init.ChemReactorBlockEntities;
import com.sazdan.chemreactor.init.ChemReactorBlocks;
import com.sazdan.chemreactor.init.ChemReactorItems;
import com.sazdan.chemreactor.init.ChemReactorMenus;
import com.sazdan.chemreactor.init.ChemReactorRecipeSerializers;
import com.sazdan.chemreactor.init.ChemReactorRecipeTypes;
import com.sazdan.chemreactor.recipe.ChemReactorRecipeManager;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
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
        ChemReactorRecipeTypes.RECIPE_TYPES.register(modEventBus);
        ChemReactorRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);
        ChemReactorMenus.MENUS.register(modEventBus);

        // Подключаем наш менеджер рецептов к общей системе Thermal: Forge сам вызовет
        // ChemReactorRecipeManager.refresh(...) при каждой (пере)загрузке рецептов —
        // ровно так же, как это происходит для BrewerRecipeManager и остальных машин Thermal.
        ThermalRecipeManagers.registerManager(ChemReactorRecipeManager.instance());

        modEventBus.addListener(this::clientSetup);
    }

    // FMLClientSetupEvent гарантированно вызывается только на клиенте — поэтому здесь,
    // а не в конструкторе, безопасно ссылаться на клиентские классы (Screen, MenuScreens)
    // без риска уронить выделенный сервер, у которого их нет на classpath.
    private void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(ChemReactorMenus.CHEM_REACTOR.get(), ChemReactorScreen::new));
    }
}
