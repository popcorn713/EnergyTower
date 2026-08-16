package com.energytower.menu;

import com.energytower.ModMenuTypes;
import com.energytower.blockentity.EnergyTowerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 能量塔控制面板菜单。
 * <p>
 * 服务端构造时持有真实方块实体；客户端构造时通过额外数据恢复 BlockPos（用于发送 C2S 包），
 * 界面数据（速率 / 模式 / 能量 / 绑定数）通过 {@link DataSlot} 由服务端每 Tick 同步到客户端。
 * <p>
 * 菜单本身不做任何数值计算，仅做展示与转发操作请求，真正的写入由服务端完成。
 */
public class EnergyTowerMenu extends AbstractContainerMenu {

    /** 数据槽索引 */
    private static final int IDX_RATE_HI = 0;
    private static final int IDX_RATE_LO = 1;
    private static final int IDX_MODE = 2;
    private static final int IDX_ENERGY_HI = 3;
    private static final int IDX_ENERGY_LO = 4;
    private static final int IDX_BINDINGS = 5;

    /** 服务端持有的塔实体（客户端为 null） */
    @Nullable
    private final EnergyTowerBlockEntity tower;
    /** 塔的坐标（客户端发送 C2S 包用） */
    private final BlockPos towerPos;

    // ===== 客户端数据槽缓存 =====
    private int rate;
    private boolean unlimited;
    private int energy;
    private int bindings;

    /** 服务端构造 */
    public EnergyTowerMenu(int id, Inventory inv, EnergyTowerBlockEntity tower) {
        super(ModMenuTypes.ENERGY_TOWER.get(), id);
        this.tower = tower;
        this.towerPos = tower.getBlockPos();
        addTowerDataSlots();
    }

    /** 客户端构造：从额外数据（IPlayerExtension.openMenu 写入的 BlockPos）恢复塔坐标 */
    public EnergyTowerMenu(int id, Inventory inv, RegistryFriendlyByteBuf data) {
        super(ModMenuTypes.ENERGY_TOWER.get(), id);
        this.tower = null;
        this.towerPos = data.readBlockPos();
        addTowerDataSlots();
    }

    /**
     * 注册数据槽：服务端每 Tick 调用 {@link #broadcastChanges()} 时，
     * 若槽值变化会以 ClientboundContainerSetDataPacket 发送给客户端，客户端调用槽的 set() 更新缓存。
     */
    private void addTowerDataSlots() {
        // 0/1：传输速率拆成高/低 16 位（速率上限 400000 > 32767，单槽放不下；
        //      数据槽经 ClientboundContainerSetDataPacket 以带符号 short 传输）
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (tower != null ? tower.getTransferRate() : rate) >>> 16;
            }

            @Override
            public void set(int value) {
                rate = (rate & 0xFFFF) | (value << 16);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (tower != null ? tower.getTransferRate() : rate) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                rate = (rate & 0xFFFF0000) | (value & 0xFFFF);
            }
        });
        // 2：传输模式（0=速率限制，1=无上限）
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return tower != null && tower.isUnlimitedMode() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                unlimited = value != 0;
            }
        });
        // 3/4：能量值拆成高低 16 位（能量最大 1000 万，超出 short 范围）
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (tower != null ? tower.getEnergyStorage().getEnergyStored() : energy) >>> 16;
            }

            @Override
            public void set(int value) {
                energy = (energy & 0xFFFF) | (value << 16);
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (tower != null ? tower.getEnergyStorage().getEnergyStored() : energy) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                energy = (energy & 0xFFFF0000) | (value & 0xFFFF);
            }
        });
        // 5：绑定设备数量
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return tower != null ? tower.getBindingCount() : bindings;
            }

            @Override
            public void set(int value) {
                bindings = value;
            }
        });
    }

    // ===== 客户端读取展示数据 =====

    public int getTransferRate() {
        return rate;
    }

    public boolean isUnlimitedMode() {
        return unlimited;
    }

    public int getEnergyStored() {
        return energy;
    }

    public int getMaxEnergy() {
        return EnergyTowerBlockEntity.MAX_ENERGY;
    }

    public int getBindingCount() {
        return bindings;
    }

    public BlockPos getTowerPos() {
        return towerPos;
    }

    @Override
    public boolean stillValid(Player player) {
        // 玩家须在塔 8 格范围内，否则自动关闭 GUI（防作弊）
        return player.distanceToSqr(towerPos.getCenter()) <= 8.0 * 8.0;
    }

    /** 本菜单没有物品槽，转移堆叠直接返回空 */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
