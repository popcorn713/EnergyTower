package com.energytower.block;

import com.energytower.ModBlockEntities;
import com.energytower.blockentity.EnergyTowerBlockEntity;
import com.energytower.menu.EnergyTowerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 能量塔方块。
 * <ul>
 *   <li>空手右键打开控制面板（GUI）；</li>
 *   <li>持有能量链路扳手时由扳手接管交互逻辑；</li>
 *   <li>注册服务端 Tick（无线供电核心逻辑）。</li>
 * </ul>
 */
public class EnergyTowerBlock extends Block implements EntityBlock {

    public EnergyTowerBlock(Properties properties) {
        super(properties);
    }

    /** 空手右键：打开控制面板 */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof EnergyTowerBlockEntity tower) {
            openMenu(player, tower);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** 供扳手与空手右键共用：以携带 BlockPos 额外数据的方式打开 GUI */
    public static void openMenu(Player player, EnergyTowerBlockEntity tower) {
        BlockPos pos = tower.getBlockPos();
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("block.energy_tower.energy_tower");
            }

            @Override
            public AbstractContainerMenu createMenu(int id, Inventory inv, Player p) {
                return new EnergyTowerMenu(id, inv, tower);
            }
        }, pos); // IPlayerExtension.openMenu(provider, pos)：把 BlockPos 写入额外数据，客户端据此恢复塔实体
    }

    /** 创建方块实体 */
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyTowerBlockEntity(pos, state);
    }

    /** 注册服务端 Tick（仅服务端生效） */
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        // 仅当方块实体类型匹配时返回我们的 Tick 逻辑
        if (type == ModBlockEntities.ENERGY_TOWER.get()) {
            BlockEntityTicker<EnergyTowerBlockEntity> typed = EnergyTowerBlockEntity::tickServer;
            return (BlockEntityTicker<T>) (BlockEntityTicker<?>) typed;
        }
        return null;
    }
}
