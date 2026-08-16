package com.energytower;

import com.energytower.menu.EnergyTowerScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * 客户端专用事件（纯客户端：GUI 屏幕注册）。
 * <p>
 * 注意：本类引用客户端类（Screen），因此只能在客户端加载。
 * 由主类在 {@code FMLEnvironment.dist == Dist.CLIENT} 时才调用 {@link #register(IEventBus)}，
 * 服务端不会加载本类。
 */
public final class ModClientEvents {

    private ModClientEvents() {
    }

    /** 在 MOD 事件总线上注册客户端监听器（主类在客户端侧调用） */
    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModClientEvents::registerMenuScreens);
    }

    /** 注册能量塔控制面板屏幕 */
    private static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.ENERGY_TOWER.get(), EnergyTowerScreen::new);
    }
}
