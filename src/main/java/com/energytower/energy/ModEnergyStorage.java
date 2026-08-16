package com.energytower.energy;

import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.energy.EnergyStorage;

/**
 * 可持久化的 FE 能量存储。
 * <p>
 * 基于 NeoForge 标准 {@link EnergyStorage}（标准 FE 接口），并额外提供：
 * <ul>
 *   <li>变化回调（{@code onChanged}）：能量变动时通知方块实体标记存档 / 同步；</li>
 *   <li>{@link #consume}：无线传输专用，直接从缓存扣除能量，不受 maxExtract 限制；</li>
 *   <li>{@link #save} / {@link #load}：NBT 读写，配合方块实体持久化。</li>
 * </ul>
 * <b>注意：</b>能量塔不会自产电，只能 {@code receive}（外部线缆/机器输入），
 * 输出走无线传输（{@link #consume}），maxExtract 固定为 0，禁止被线缆直接抽取。
 */
public class ModEnergyStorage extends EnergyStorage {

    /** 能量发生变化时的回调（服务端：标记存档 / 记录脏标记） */
    private final Runnable onChanged;

    public ModEnergyStorage(int capacity, int maxReceive, int maxExtract, Runnable onChanged) {
        super(capacity, maxReceive, maxExtract);
        this.onChanged = onChanged;
    }

    /** 每次成功接收能量后触发回调（用于标记存档 / 同步） */
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);
        if (received > 0 && !simulate) {
            onChanged.run();
        }
        return received;
    }

    /** 每次成功抽出能量后触发回调（本模组 maxExtract=0，正常不会触发） */
    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);
        if (extracted > 0 && !simulate) {
            onChanged.run();
        }
        return extracted;
    }

    /**
     * 无线传输专用：直接从缓存扣除最多 {@code amount} 点 FE，不受 maxExtract 限制。
     *
     * @return 实际扣除的 FE 数量
     */
    public int consume(int amount, boolean simulate) {
        int actual = Math.max(0, Math.min(amount, energy));
        if (!simulate && actual > 0) {
            energy -= actual;
            onChanged.run();
        }
        return actual;
    }

    /** 读取当前缓存能量（供渲染 / GUI 展示） */
    public int getEnergyStored() {
        return energy;
    }

    /** 读取缓存容量 */
    public int getMaxEnergyStored() {
        return capacity;
    }

    /** 写入 NBT */
    public void save(CompoundTag tag) {
        tag.putInt("Energy", energy);
    }

    /** 从 NBT 读取（带范围钳制，防脏数据） */
    public void load(CompoundTag tag) {
        energy = Math.max(0, Math.min(capacity, tag.getInt("Energy")));
    }
}
