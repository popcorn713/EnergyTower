package com.energytower;

import com.energytower.menu.EnergyTowerMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 菜单（GUI）类型注册。
 * <p>
 * 使用 {@link IMenuTypeExtension#create} 创建支持额外数据的 MenuType，
 * 客户端通过 `FriendlyByteBuf` 恢复能量塔的 BlockPos（由 IPlayerExtension.openMenu(provider, pos) 写入）。
 */
public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EnergyTower.MODID);

    /** 能量塔控制面板菜单 */
    public static final DeferredHolder<MenuType<?>, MenuType<EnergyTowerMenu>> ENERGY_TOWER =
            MENUS.register("energy_tower", () -> IMenuTypeExtension.create(EnergyTowerMenu::new));
}
