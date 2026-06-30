package com.sazdan.chemreactor.init;

import cofh.core.util.ProxyUtils;
import com.sazdan.chemreactor.ChemReactorAddon;
import com.sazdan.chemreactor.inventory.ChemReactorContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Регистрация MenuType — "ярлык", по которому клиент понимает, какой Container
// пересобрать у себя при получении пакета открытия окна с сервера.
// IForgeMenuType.create(...) даёт фабрику (windowId, playerInv, data) -> Container;
// "data" — это BlockPos, который NetworkHooks.openScreen(...) сам допишет в пакет
// (см. ChemReactorBlock.use). ProxyUtils.getClientWorld()/getClientPlayer() — готовые
// CoFH-хелперы для получения клиентского Level/Player здесь же, без своих DistExecutor'ов
// (так же делает сам Thermal в TExpContainers).
public class ChemReactorMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ChemReactorAddon.MOD_ID);

    public static final RegistryObject<MenuType<ChemReactorContainer>> CHEM_REACTOR =
            MENUS.register("chemical_reactor", () -> IForgeMenuType.create(
                    (windowId, inv, data) -> new ChemReactorContainer(
                            windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), inv, ProxyUtils.getClientPlayer())
            ));
}
