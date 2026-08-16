package com.energytower.client;

import com.energytower.EnergyTower;
import com.energytower.block.EnergyTowerBlock;
import com.energytower.item.EnergyLinkWrenchItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.List;

/**
 * 扳手高亮渲染（纯客户端）：手持能量链路扳手时，
 * 用线框高亮「已选能量塔」与「绑定到该塔的所有机器」。
 * <p>
 * 数据来自 {@link ClientWrenchData}（由服务端 S2C 包同步），渲染本身不做任何逻辑判定。
 */
@EventBusSubscriber(modid = EnergyTower.MODID, value = Dist.CLIENT)
public final class WrenchHighlightRenderer {

    /** 高亮最大渲染距离（格），超出的目标不画，减少渲染开销 */
    private static final int RENDER_DIST = 64;
    private static final int RENDER_DIST_SQ = RENDER_DIST * RENDER_DIST;

    private WrenchHighlightRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        // 在半透明方块之后绘制，确保线框清晰可见
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Player player = Minecraft.getInstance().player;
        if (player == null || !holdingWrench(player)) {
            return;
        }
        List<BlockPos> targets = ClientWrenchData.getBoundTargets();
        BlockPos tower = ClientWrenchData.getSelectedTower();
        if (tower == null && targets.isEmpty()) {
            return;
        }
        // 已选塔：超出距离或被拆除（不再是自己方块）则跳过
        if (tower != null
                && (player.distanceToSqr(Vec3.atCenterOf(tower)) > RENDER_DIST_SQ
                    || !(player.level().getBlockState(tower).getBlock() instanceof EnergyTowerBlock))) {
            tower = null;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cam = event.getCamera().getPosition();
        // 1.21.1 的 RenderLevelStageEvent 不直接暴露 MultiBufferSource，使用全局缓冲源（帧末统一 flush）
        VertexConsumer lines = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z); // 回到世界坐标

        // 已选能量塔：青色
        if (tower != null) {
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(tower), 0.2F, 0.9F, 1.0F, 0.9F);
        }
        // 绑定机器：绿色（跳过空气方块与超远目标——被拆掉的机器自动不再高亮，且几乎零额外开销）
        for (BlockPos p : targets) {
            if (player.distanceToSqr(Vec3.atCenterOf(p)) > RENDER_DIST_SQ) {
                continue;
            }
            if (player.level().getBlockState(p).isAir()) {
                continue;
            }
            LevelRenderer.renderLineBox(poseStack, lines, new AABB(p), 0.3F, 1.0F, 0.4F, 0.9F);
        }
        poseStack.popPose();
    }

    private static boolean holdingWrench(Player player) {
        return player.getMainHandItem().getItem() instanceof EnergyLinkWrenchItem
                || player.getOffhandItem().getItem() instanceof EnergyLinkWrenchItem;
    }
}
