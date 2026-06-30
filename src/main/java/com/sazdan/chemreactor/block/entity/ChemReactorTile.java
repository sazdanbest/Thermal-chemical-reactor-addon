package com.sazdan.chemreactor.block.entity;

import cofh.lib.api.StorageGroup;
import cofh.lib.fluid.FluidStorageCoFH;
import cofh.lib.inventory.ItemStorageCoFH;
import cofh.thermal.lib.block.entity.MachineBlockEntity;
import com.sazdan.chemreactor.init.ChemReactorBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import javax.annotation.Nullable;

// ChemReactorTile — это "мозг" блока: вся реальная логика машины живёт здесь,
// а не в ChemReactorBlock (блок — это просто физическая оболочка в мире).
//
// Наследуясь от MachineBlockEntity, мы автоматически получаем:
//   - управление RF-энергией (energyStorage)
//   - систему аугментов (слоты усиления, бонусы скорости/энергоэффективности)
//   - редстоун-контроль (можно настроить реакцию на сигнал редстоуна)
//   - конфигурацию сторон блока (вход/выход для авто-извлечения предметов трубами и т.п.)
//
// На ЭТОМ шаге (3) мы НЕ подключаем рецепты (cacheRecipe() возвращает false по
// умолчанию из родителя — машина физически не сможет начать обработку,
// но Tile уже полностью функционален с точки зрения хранения предметов/жидкостей/энергии).
public class ChemReactorTile extends MachineBlockEntity {

    public ChemReactorTile(BlockPos pos, BlockState state) {
        // Передаём BlockEntityType, зарегистрированный в ChemReactorBlockEntities.
        // .get() распаковывает RegistryObject в реальный объект BlockEntityType.
        super(ChemReactorBlockEntities.CHEMICAL_REACTOR.get(), pos, state);

        // --- Настройка слотов под предметы (item slots) ---
        // Согласно спецификации: 2 входных слота, 2 выходных слота.
        // StorageGroup.INPUT / OUTPUT — это то, что отличает "куда можно положить руками
        // и откуда труба может забрать" от "куда труба может только класть".
        this.inventory.addSlot(new ItemStorageCoFH(), StorageGroup.INPUT);
        this.inventory.addSlot(new ItemStorageCoFH(), StorageGroup.INPUT);
        this.inventory.addSlot(new ItemStorageCoFH(), StorageGroup.OUTPUT);
        this.inventory.addSlot(new ItemStorageCoFH(), StorageGroup.OUTPUT);

        // --- Настройка баков под жидкости (fluid tanks) ---
        // 2 входных бака по 4000 mB, 1 выходной бак на 8000 mB — как в спецификации.
        this.tankInv.addTank(new FluidStorageCoFH(4000), StorageGroup.INPUT);
        this.tankInv.addTank(new FluidStorageCoFH(4000), StorageGroup.INPUT);
        this.tankInv.addTank(new FluidStorageCoFH(8000), StorageGroup.OUTPUT);

        // Обязательный финальный шаг после того как все слоты/баки добавлены —
        // без этого вызова инвентарь и баки не построят свои IItemHandler/IFluidHandler
        // (они создаются "лениво" через initHandlers(), и мы должны вызвать его
        // явно один раз сразу после настройки структуры).
        this.initHandlers();
    }

    // --- Тайминг и энергия машины ---
    // Переопределяем дефолтные 20 тиков (1 секунда) на наши 800 тиков (40 секунд),
    // как указано в спецификации. Это ВРЕМЯ ОДНОГО ПОЛНОГО ЦИКЛА обработки при
    // базовой скорости (без аугментов ускорения).
    @Override
    protected int getBaseProcessTick() {
        return 800;
    }

    // Хранилище опыта пока не используем (XP не выдаётся этой машиной) —
    // оставляем дефолтное значение родителя.
    // (getBaseXpStorage() не переопределяем, родитель даст 2500 по умолчанию,
    // но т.к. xpStorageFeature отключен по конфигу, на практике это не используется)

    // --- Временная заглушка для MenuProvider ---
    // AugmentableBlockEntity (предок по иерархии) реализует интерфейс MenuProvider
    // целиком, включая getDisplayName() — переопределять его нам не нужно.
    // Но createMenu() в родителе abstract/не задан содержательно для машины без
    // GUI, поэтому переопределяем именно его. GUI появится на шаге 5-6 — пока
    // возвращаем null, что означает "у тайла нет открываемого меню". Блок при
    // этом полностью работает физически (хранит предметы/жидкости/энергию),
    // просто правый клик пока не откроет интерфейс.
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        return null;
    }
}