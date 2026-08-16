package com.energytower;

import com.energytower.blockentity.EnergyTowerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块实体注册类，并在此向外部暴露 FE 能量能力（NeoForge 标准能力系统）。
 */
public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, EnergyTower.MODID);

    /** 能量塔方块实体 */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyTowerBlockEntity>> ENERGY_TOWER =
            BLOCK_ENTITIES.register("energy_tower",
                    () -> BlockEntityType.Builder.of(EnergyTowerBlockEntity::new, ModBlocks.ENERGY_TOWER.get())
                            .build(null));

    /**
     * 注册方块实体能力：任何使用 NeoForge 标准 FE 接口（IEnergyStorage / Capabilities.EnergyStorage.BLOCK）
     * 的线缆、机器都能向能量塔输入电能。
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ENERGY_TOWER.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
    }
}
