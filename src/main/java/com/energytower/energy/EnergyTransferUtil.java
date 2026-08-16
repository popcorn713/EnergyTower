package com.energytower.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

/**
 * 标准 FE 能力的通用探测工具（服务端使用）。
 * <p>
 * 为什么需要“模拟探测”：Mekanism 等模组的 FE 包装器 {@code canReceive()} 恒为 true，
 * 但不同方向受机器“能量方向配置”（输入/输出/禁用）与自动化类型影响，真正能否充电
 * 只能靠 {@code receiveEnergy(amount, true)}（模拟，无副作用）来判断。
 */
public final class EnergyTransferUtil {

    /** 绑定/校验“是否可接收”时使用的探测量（要足够大，避免 Mek 的 FE↔J 取整把结果归零） */
    private static final int PROBE_AMOUNT = 1_000;

    private EnergyTransferUtil() {
    }

    /**
     * 返回目标方块“最能接收能量”的 FE 存储：
     * 对 无方向 + 6 个方向 逐个模拟探测，取模拟接收量最大的那个。
     *
     * @return 可接收的存储；目标不可用/全部拒收时返回 null
     */
    @Nullable
    public static IEnergyStorage findBestReceivingStorage(Level level, BlockPos pos, int attempt) {
        if (!level.isLoaded(pos)) {
            return null;
        }
        IEnergyStorage best = null;
        int bestAccepted = 0;
        // 无方向（内部/通用）
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (storage != null) {
            int accepted = probeReceive(storage, attempt);
            if (accepted > bestAccepted) {
                best = storage;
                bestAccepted = accepted;
            }
        }
        // 6 个方向逐个探测
        for (Direction dir : Direction.values()) {
            storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
            if (storage == null) {
                continue;
            }
            int accepted = probeReceive(storage, attempt);
            if (accepted > bestAccepted) {
                best = storage;
                bestAccepted = accepted;
            }
        }
        return best;
    }

    /**
     * 判断目标方块是否是“可接收 FE 的机器”（绑定校验用）。
     * 只要任一方向能接收（或声明 canReceive）即认为支持。
     */
    public static boolean supportsEnergyReceive(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return false;
        }
        IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
        if (storage != null && accepts(storage)) {
            return true;
        }
        for (Direction dir : Direction.values()) {
            storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
            if (storage != null && accepts(storage)) {
                return true;
            }
        }
        return false;
    }

    /** canReceive 或 模拟探测能收到能量，都视为“可接收” */
    private static boolean accepts(IEnergyStorage storage) {
        try {
            return storage.canReceive() || probeReceive(storage, PROBE_AMOUNT) > 0;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** 安全探测：模拟接收，能收多少返回多少；异常/不可接收返回 0 */
    private static int probeReceive(IEnergyStorage storage, int amount) {
        try {
            if (!storage.canReceive()) {
                return 0;
            }
            return Math.max(0, storage.receiveEnergy(amount, true));
        } catch (RuntimeException ex) {
            return 0;
        }
    }
}
