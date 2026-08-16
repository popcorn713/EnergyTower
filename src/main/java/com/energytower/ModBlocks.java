package com.energytower;

import com.energytower.block.EnergyTowerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册类（NeoForge 1.21.1 DeferredRegister.Blocks 新注册机制）。
 */
public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(EnergyTower.MODID);

    /** 能量塔：储能 + 无线供电方块 */
    public static final DeferredBlock<Block> ENERGY_TOWER = BLOCKS.registerBlock(
            "energy_tower",
            EnergyTowerBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 6.0F)
                    .sound(SoundType.METAL));
}
