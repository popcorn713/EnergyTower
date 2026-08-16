package com.energytower;

import com.energytower.network.ModNetwork;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * 能量塔科技模组主类。
 * <p>
 * 内容：
 * <ul>
 *   <li>能量塔：储能方块，接收外部 FE 输入，内置能量缓存，支持无线向外输送能量；</li>
 *   <li>能量链路扳手：用于选定能量塔、绑定/解绑无线供电目标、打开控制面板。</li>
 * </ul>
 * 所有能量计算、绑定关系、无线传输逻辑全部运行在服务端，客户端仅负责渲染、粒子与 GUI。
 */
@Mod(EnergyTower.MODID)
public class EnergyTower {

    public static final String MODID = "energy_tower";

    public EnergyTower(IEventBus modEventBus, ModContainer container) {
        // 注册方块、物品、方块实体、菜单、创造标签、附件类型（MOD 事件总线）
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModAttachments.register(modEventBus);

        // 客户端专用事件（屏幕 / 渲染器）：仅客户端加载，服务端跳过
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModClientEvents.register(modEventBus);
        }

        // 网络载荷注册（RegisterPayloadHandlersEvent 是 MOD 总线事件）
        modEventBus.addListener(ModNetwork::register);
        // 方块实体 FE 能力注册（RegisterCapabilitiesEvent 也是 MOD 总线事件，
        // 必须注册在 modEventBus 上，不能挂到 NeoForge.EVENT_BUS 通用总线）
        modEventBus.addListener(ModBlockEntities::registerCapabilities);
    }
}
