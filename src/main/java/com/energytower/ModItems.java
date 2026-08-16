package com.energytower;

import com.energytower.item.EnergyLinkWrenchItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册类（NeoForge 1.21.1 DeferredRegister.Items 新注册机制）。
 */
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(EnergyTower.MODID);

    /** 能量塔方块对应的方块物品（同名自动注册） */
    public static final DeferredItem<BlockItem> ENERGY_TOWER = ITEMS.registerSimpleBlockItem(ModBlocks.ENERGY_TOWER);

    /** 能量链路扳手：选定发射源 / 绑定 / 解绑 / 打开控制面板 */
    public static final DeferredItem<EnergyLinkWrenchItem> ENERGY_LINK_WRENCH = ITEMS.registerItem(
            "energy_link_wrench",
            EnergyLinkWrenchItem::new,
            new Item.Properties().stacksTo(1));
}
