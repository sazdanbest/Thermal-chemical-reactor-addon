package com.sazdan.chemreactor.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

// На этом шаге (шаг 2) ChemReactorBlock — это ПРОСТОЙ блок без какой-либо
// логики машины. Это сделано намеренно: сначала убеждаемся, что блок
// корректно регистрируется, у него есть модель и его можно поставить в мире.
//
// На шаге 3 мы заменим этот класс на полноценный EntityBlock с привязкой
// к ChemReactorTile (наследнику MachineBlockEntity из CoFH/Thermal) —
// именно тогда появится вся логика: GUI, рецепты, энергия и т.д.
public class ChemReactorBlock extends Block {

    public ChemReactorBlock() {
        super(
                // Properties — это "паспорт" физических свойств блока.
                // Берём Material.METAL — машины Thermal сделаны из металла,
                // это влияет на звук, цвет на карте и то, что блок ломается киркой.
                BlockBehaviour.Properties.of(Material.METAL, MaterialColor.METAL)
                        .strength(3.5f, 8.0f)   // (твёрдость, устойчивость к взрыву) — как у большинства машин Thermal
                        .sound(SoundType.METAL) // звук шагов/разрушения — металлический
                        .requiresCorrectToolForDrops() // блок ломается только подходящим инструментом (киркой)
        );
    }
}
