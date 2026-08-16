package com.energytower.client;

import com.energytower.network.C2SMassBindPacket;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 扳手在客户端的交互辅助（纯客户端，服务端不会加载执行）。
 * <p>
 * 当前用途：检测 Shift+Ctrl+右键 → 发送「一键批量绑定相邻同种机器」请求。
 * 服务端仍会自行校验，客户端只负责把意图告诉服务端。
 */
public final class ClientWrenchHelper {

    private ClientWrenchHelper() {
    }

    /**
     * 在客户端右键处理时调用：若玩家按住 Shift+Ctrl，发送批量绑定请求并返回 true。
     *
     * @return true 表示已发送批量绑定请求（调用方应直接返回成功，不继续走普通逻辑）
     */
    public static boolean trySendMassBind(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown() && Screen.hasControlDown()) {
            PacketDistributor.sendToServer(new C2SMassBindPacket(context.getClickedPos()));
            return true;
        }
        return false;
    }
}
