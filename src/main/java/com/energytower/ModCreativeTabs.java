package com.energytower;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造标签页注册：集中收纳能量塔与能量链路扳手。
 */
public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EnergyTower.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ENERGY_TAB =
            CREATIVE_TABS.register("energy_tower", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.energy_tower"))
                    .icon(() -> new ItemStack(ModBlocks.ENERGY_TOWER.asItem()))
                    .displayItems((params, output) -> {
                        output.accept(ModBlocks.ENERGY_TOWER.asItem());
                        output.accept(ModItems.ENERGY_LINK_WRENCH.get());
                    })
                    .build());
}
