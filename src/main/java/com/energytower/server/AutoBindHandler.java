package com.energytower.server;

import com.energytower.block.EnergyTowerBlock;
import com.energytower.blockentity.EnergyTowerBlockEntity;
import com.energytower.energy.EnergyTransferUtil;
import com.energytower.item.EnergyLinkWrenchItem;
import com.energytower.network.S2CWrenchHighlightPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/**
 * 自动绑定处理器（服务端逻辑，事件在游戏总线上）。
 * <p>
 * 功能：当玩家【副手】手持能量链路扳手时，随后【放置】的机器会自动绑定到
 * 该玩家当前选中的能量塔（无需再手动 Shift+右键 绑定）。
 */
@EventBusSubscriber(modid = com.energytower.EnergyTower.MODID)
public final class AutoBindHandler {

    private AutoBindHandler() {
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        // 只处理服务端；getLevel() 实际是 Level，这里做安全转换
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        // 排除放置能量塔自身
        if (event.getPlacedBlock().getBlock() instanceof EnergyTowerBlock) {
            return;
        }
        // 副手必须拿着能量链路扳手
        if (!(player.getOffhandItem().getItem() instanceof EnergyLinkWrenchItem)) {
            return;
        }
        // 必须已选定能量塔且同维度
        WrenchSelectionManager.TowerSelection sel = WrenchSelectionManager.get(player);
        if (sel == null || !sel.dimension().equals(level.dimension())) {
            return;
        }
        if (!(level.getBlockEntity(sel.pos()) instanceof EnergyTowerBlockEntity tower)) {
            WrenchSelectionManager.clear(player);
            return;
        }
        BlockPos placed = event.getPos();
        // 放置的是可接收 FE 的机器 → 自动绑定
        if (EnergyTransferUtil.supportsEnergyReceive(level, placed) && tower.addBinding(level, placed)) {
            player.displayClientMessage(Component.translatable("message.energy_tower.auto_bound"), true);
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new S2CWrenchHighlightPacket(
                        Optional.of(tower.getBlockPos()), List.copyOf(tower.getBoundTargets())));
            }
        }
    }
}
