package com.energytower.item;

import com.energytower.block.EnergyTowerBlock;
import com.energytower.blockentity.EnergyTowerBlockEntity;
import com.energytower.client.ClientWrenchHelper;
import com.energytower.energy.EnergyTransferUtil;
import com.energytower.network.S2CWrenchHighlightPacket;
import com.energytower.server.MassBindTracker;
import com.energytower.server.WrenchSelectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * 能量链路扳手 —— 无线供电配置工具。
 * <p>
 * <b>操作规则（全部在服务端判定）：</b>
 * <ul>
 *   <li>【Shift+右键】能量塔：选定为无线发射源（清空全部绑定已移到控制面板 GUI，带二次确认）；</li>
 *   <li>【Shift+右键】机器：若已选发射源 → 绑定该机器（重复则单独解除绑定）；</li>
 *   <li>【Shift+Ctrl+右键】机器：一键批量绑定所有相邻（相连）的同种机器；</li>
 *   <li>【普通右键】能量塔：打开控制面板 GUI。</li>
 * </ul>
 */
public class EnergyLinkWrenchItem extends Item {

    public EnergyLinkWrenchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        // 客户端：检测 Shift+Ctrl+右键（一键批量绑定同种机器），发送请求后直接返回成功
        if (level.isClientSide) {
            ClientWrenchHelper.trySendMassBind(context);
            return InteractionResult.sidedSuccess(true);
        }

        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.sidedSuccess(true);
        }

        BlockPos clickedPos = context.getClickedPos();
        BlockEntity be = level.getBlockEntity(clickedPos);
        boolean sneaking = player.isShiftKeyDown();

        if (be instanceof EnergyTowerBlockEntity tower) {
            handleTower(player, level, tower, sneaking);
        } else {
            handleMachine(player, level, clickedPos, sneaking);
        }
        return InteractionResult.sidedSuccess(true);
    }

    /** 处理对能量塔的点击：选定发射源 / 打开 GUI */
    private void handleTower(Player player, Level level, EnergyTowerBlockEntity tower, boolean sneaking) {
        BlockPos pos = tower.getBlockPos();
        if (sneaking) {
            // Shift+右键：始终选定此塔为无线发射源（清空绑定已移到 GUI，带二次确认）
            WrenchSelectionManager.select(player, level.dimension(), pos);
            sendMessage(player, "message.energy_tower.tower_selected");
            // 同步高亮：已选塔 + 当前绑定机器
            syncHighlight(player, tower);
        } else {
            // 普通右键 → 打开控制面板
            EnergyTowerBlock.openMenu(player, tower);
            sendMessage(player, "message.energy_tower.opened_gui");
        }
    }

    /** 处理对普通方块的点击：绑定 / 解除绑定无线目标 */
    private void handleMachine(Player player, Level level, BlockPos clickedPos, boolean sneaking) {
        if (!sneaking) {
            sendMessage(player, "message.energy_tower.use_sneak_to_bind");
            return;
        }
        WrenchSelectionManager.TowerSelection selection = WrenchSelectionManager.get(player);
        if (selection == null) {
            sendMessage(player, "message.energy_tower.no_tower_selected");
            return;
        }
        // 维度校验：所选塔必须在当前维度
        if (!selection.dimension().equals(level.dimension())) {
            WrenchSelectionManager.clear(player);
            sendMessage(player, "message.energy_tower.no_tower_selected");
            return;
        }
        // 校验所选塔仍然存在
        if (!(level.getBlockEntity(selection.pos()) instanceof EnergyTowerBlockEntity tower)) {
            WrenchSelectionManager.clear(player);
            sendMessage(player, "message.energy_tower.tower_missing");
            return;
        }
        // 校验目标支持标准 FE 能量接收（NeoForge 标准能力，兼容所有科技模组机器）
        if (!EnergyTransferUtil.supportsEnergyReceive(level, clickedPos)) {
            sendMessage(player, "message.energy_tower.not_energy_receiver");
            return;
        }
        // 该位置刚被“一键批量绑定”处理过（本次点击就是批量绑定的一部分）：跳过普通绑定/解绑，
        // 否则普通右键会把刚被批量绑定（尤其是点中）的机器又解绑掉。
        if (MassBindTracker.isRecent(clickedPos, level.getGameTime())) {
            return;
        }
        // 已绑定 → 解除；未绑定 → 绑定
        if (tower.hasBinding(clickedPos)) {
            tower.removeBinding(clickedPos);
            sendMessage(player, "message.energy_tower.binding_removed");
        } else {
            boolean ok = tower.addBinding(level, clickedPos);
            sendMessage(player, ok ? "message.energy_tower.bound" : "message.energy_tower.binding_failed");
        }
        // 同步高亮：更新绑定机器清单
        syncHighlight(player, tower);
    }

    /** 把高亮数据（已选塔 + 绑定机器）同步给该玩家客户端（仅服务端调用） */
    private void syncHighlight(Player player, EnergyTowerBlockEntity tower) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new S2CWrenchHighlightPacket(
                    Optional.of(tower.getBlockPos()),
                    List.copyOf(tower.getBoundTargets())));
        }
    }

    private void sendMessage(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.energy_tower.energy_link_wrench.tip1"));
        tooltipComponents.add(Component.translatable("item.energy_tower.energy_link_wrench.tip2"));
        tooltipComponents.add(Component.translatable("item.energy_tower.energy_link_wrench.tip3"));
        tooltipComponents.add(Component.translatable("item.energy_tower.energy_link_wrench.tip4"));
        tooltipComponents.add(Component.translatable("item.energy_tower.energy_link_wrench.tip5"));
    }
}
