package com.energytower.blockentity;

import com.energytower.ModAttachments;
import com.energytower.ModBlockEntities;
import com.energytower.energy.EnergyTransferUtil;
import com.energytower.energy.ModEnergyStorage;
import com.energytower.network.S2CWrenchHighlightPacket;
import com.energytower.server.WrenchSelectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 能量塔方块实体 —— 模组核心。
 * <p>
 * <b>功能：</b>
 * <ol>
 *   <li>内置能量缓存，可接收任何使用 NeoForge 标准 FE 接口（线缆 / 机器）输入的电能；</li>
 *   <li>存储电能后，每 Tick 向绑定的无线目标（同样使用标准 FE 接口）分发能量，无需电线；</li>
 *   <li>电量耗尽立即停止无线供电，必须由外部持续补给。</li>
 * </ol>
 * <b>设计约束：</b>
 * <ul>
 *   <li>所有能量计算、绑定关系、无线传输逻辑全部在服务端执行；</li>
 *   <li>绑定清单、传输参数通过 NBT 持久化，退出重进世界不丢失；</li>
 *   <li>无线传输不设最大距离限制（同维度任意位置）。</li>
 * </ul>
 */
public class EnergyTowerBlockEntity extends BlockEntity {

    /** 日志（诊断无线供电问题用，可随时关闭 DEBUG_LOG） */
    private static final Logger LOGGER = LoggerFactory.getLogger(EnergyTowerBlockEntity.class);
    /** 是否输出诊断日志（排查机器无法充电时开启，确认后改回 false） */
    private static final boolean DEBUG_LOG = true;

    // ====================== 可调常量（集中管理，方便后续扩展/配置化） ======================
    /** 内置储能容量（FE），10,000,000 */
    public static final int MAX_ENERGY = 10_000_000;
    /** 每 Tick 最大接收外部输入（FE）。Integer.MAX_VALUE = 输入速度无上限（仅受剩余缓存限制） */
    public static final int MAX_INPUT = Integer.MAX_VALUE;
    /** 默认无线单次传输速率（FE / Tick / 台） */
    public static final int DEFAULT_RATE = 1_000;
    /** 无线传输速率硬上限（服务端接受的最大值；GUI 滑块只到 32000，自定义输入可到该值） */
    public static final int MAX_RATE = 400_000;
    /** 无线传输时的粒子间隔（Tick） */
    private static final int PARTICLE_INTERVAL = 20;
    /** 绑定目标“最佳接收存储”缓存刷新间隔（Tick，1 秒一次，减少每 Tick 能力查询开销） */
    private static final int PROBE_INTERVAL = 20;

    /** 能量存储 */
    private final ModEnergyStorage energyStorage;
    /** 绑定目标清单（坐标 + 机器方块实体上的唯一绑定 ID，用于精确识别“同一台机器”） */
    private final List<BoundTarget> boundTargets = new ArrayList<>();
    /** 无线单次传输速率（仅速率限制模式生效） */
    private int transferRate = DEFAULT_RATE;
    /** true=无上限传输模式，false=速率限制模式 */
    private boolean unlimitedMode = false;

    /** 绑定目标 → 探测到的“最佳接收存储”缓存（每 PROBE_INTERVAL 刷新，降低每 Tick 开销） */
    private final Map<BlockPos, IEnergyStorage> targetStorageCache = new HashMap<>();
    /** 上次刷新存储缓存的时间 */
    private long lastProbeTime = Long.MIN_VALUE;

    /** 诊断日志节流用：上次输出“目标拒收”的时间 */
    private long lastRejectLogTime = -100;

    /** 一个绑定目标：坐标 + 机器方块实体上的唯一绑定 ID */
    public record BoundTarget(BlockPos pos, UUID bindId) {
    }

    public EnergyTowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_TOWER.get(), pos, state);
        this.energyStorage = new ModEnergyStorage(MAX_ENERGY, MAX_INPUT, 0, this::onEnergyChanged);
    }

    public ModEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    // ============================== Tick 逻辑（仅服务端） ==============================

    /** 由方块 {@code getTicker} 调用，仅在服务端注册 */
    public static void tickServer(Level level, BlockPos pos, BlockState state, EnergyTowerBlockEntity be) {
        be.serverTick(level);
    }

    private void serverTick(Level level) {
        // 周期检查：绑定目标被破坏（方块已被替换）→ 自动断开链接
        if (level.getGameTime() % PROBE_INTERVAL == 0) {
            pruneBrokenTargets(level);
        }
        // 诊断：有绑定且有电时，每 5 秒输出一次塔的状态（方便排查充电问题）
        if (DEBUG_LOG && !boundTargets.isEmpty() && energyStorage.getEnergyStored() > 0 && level.getGameTime() % 100 == 0) {
            LOGGER.info("[EnergyTower] 塔 {} 储能 {}FE，绑定 {} 台，速率 {}FE/t，模式 {}",
                    worldPosition, energyStorage.getEnergyStored(), boundTargets.size(),
                    transferRate, unlimitedMode ? "无上限" : "速率限制");
        }
        // 电量耗尽 → 立即停止无线供电
        if (energyStorage.getEnergyStored() <= 0 || boundTargets.isEmpty()) {
            return;
        }
        distribute(level);
        // 传输中播放粒子（仅当确实在供电）
        if (level.getGameTime() % PARTICLE_INTERVAL == 0 && energyStorage.getEnergyStored() > 0) {
            spawnSparkParticles(level);
        }
    }

    /**
     * 每 Tick 无线能量分发：遍历绑定目标，向其标准 FE 能力对象发送能量。
     * <ul>
     *   <li>速率限制模式：每台机器每次最多 {@link #transferRate} FE；</li>
     *   <li>无上限传输模式：每台机器尽量接收剩余全部能量（受目标自身接收上限限制）。</li>
     * </ul>
     */
    private void distribute(Level level) {
        // 是否本轮需要重新探测目标方向（1 秒一次，其余 tick 用缓存，显著降低开销）
        boolean refresh = level.getGameTime() >= lastProbeTime + PROBE_INTERVAL;
        if (refresh) {
            lastProbeTime = level.getGameTime();
        }
        // 拷贝一份列表，防止传输过程中绑定被修改导致并发修改异常
        for (BoundTarget bt : List.copyOf(boundTargets)) {
            BlockPos target = bt.pos();
            if (energyStorage.getEnergyStored() <= 0) {
                break; // 能量耗尽，后续目标停止
            }
            // 本次尝试传输量
            int attempt = unlimitedMode ? energyStorage.getEnergyStored() : transferRate;
            if (attempt <= 0) {
                continue;
            }
            // 取“最能接收能量”的存储：优先用缓存，每 PROBE_INTERVAL 重新探测
            IEnergyStorage targetStorage = getCachedStorage(level, target, attempt, refresh);
            if (targetStorage == null) {
                logReject(level, target, attempt, "未找到可接收能量的存储");
                continue;
            }
            // 目标实际接收量 = 塔内实际消耗量（服务端权威计算）
            int received = 0;
            try {
                received = targetStorage.receiveEnergy(attempt, false);
            } catch (RuntimeException ex) {
                logReject(level, target, attempt, "接收时抛异常: " + ex);
                continue;
            }
            if (received > 0) {
                energyStorage.consume(received, false);
            } else {
                logReject(level, target, attempt, "返回 0（目标可能已满/该面被配置为输出）");
            }
        }
    }

    /**
     * 获取目标“最能接收能量”的存储：命中缓存直接返回（零能力查询），否则探测一次并缓存。
     * 探测逻辑见 {@link EnergyTransferUtil#findBestReceivingStorage}。
     */
    @Nullable
    private IEnergyStorage getCachedStorage(Level level, BlockPos target, int attempt, boolean refresh) {
        if (refresh || !targetStorageCache.containsKey(target)) {
            IEnergyStorage storage = EnergyTransferUtil.findBestReceivingStorage(level, target, attempt);
            if (storage != null) {
                targetStorageCache.put(target, storage);
            } else {
                targetStorageCache.remove(target);
            }
        }
        return targetStorageCache.get(target);
    }

    /** 诊断日志（节流：每 5 秒最多一条，避免刷屏） */
    private void logReject(Level level, BlockPos target, int attempt, String reason) {
        if (!DEBUG_LOG || level.getGameTime() < lastRejectLogTime + 100) {
            return;
        }
        lastRejectLogTime = level.getGameTime();
        LOGGER.warn("[EnergyTower] 塔 {} 给 {} 供电失败 (请求 {}FE, 原因: {}), 塔当前储能 {}FE",
                worldPosition, target, attempt, reason, energyStorage.getEnergyStored());
    }

    /** 播放无线传输粒子（服务端广播，客户端渲染） */
    private void spawnSparkParticles(Level level) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            double x = worldPosition.getX() + 0.5D;
            double y = worldPosition.getY() + 1.05D;
            double z = worldPosition.getZ() + 0.5D;
            serverLevel.sendParticles(
                    net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    x, y, z,
                    3, 0.2D, 0.2D, 0.2D, 0.02D);
        }
    }

    // ============================== 绑定管理（服务端） ==============================

    /**
     * 绑定一台机器（记录其方块类型，用于检测被破坏后自动断开）。
     *
     * @return 是否绑定成功（不能绑定自身 / 重复绑定）
     */
    public boolean addBinding(Level level, BlockPos pos) {
        if (pos.equals(this.worldPosition) || hasBinding(pos)) {
            return false;
        }
        // 给目标机器的方块实体打一个唯一绑定 ID：机器被拆后该 ID 随之消失，
        // 即使原位重放同类型机器也是新实例（无此 ID），从而能自动断开旧链接
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) {
            return false; // 必须是有方块实体的机器
        }
        UUID id = UUID.randomUUID();
        be.setData(ModAttachments.BIND_ID, id);
        be.setChanged();
        boundTargets.add(new BoundTarget(pos, id));
        targetStorageCache.remove(pos); // 新目标需重新探测
        markDirtyAndSync();
        return true;
    }

    /** 解除单台机器绑定 */
    public boolean removeBinding(BlockPos pos) {
        boolean removed = boundTargets.removeIf(bt -> bt.pos().equals(pos));
        if (removed) {
            targetStorageCache.remove(pos);
            markDirtyAndSync();
        }
        return removed;
    }

    /** 清空全部绑定 */
    public void clearBindings() {
        if (!boundTargets.isEmpty()) {
            boundTargets.clear();
            targetStorageCache.clear();
            markDirtyAndSync();
        }
    }

    public boolean hasBinding(BlockPos pos) {
        return boundTargets.stream().anyMatch(bt -> bt.pos().equals(pos));
    }

    /** 所有绑定机器的坐标（供高亮同步等使用） */
    public List<BlockPos> getBoundTargets() {
        return boundTargets.stream().map(BoundTarget::pos).toList();
    }

    /**
     * 自动断开：绑定目标被破坏（该位置的方块实体被移除，或换成了新的机器实例）时移除绑定。
     * 通过比较机器方块实体上的唯一绑定 ID 精确识别，每 PROBE_INTERVAL tick 调用一次。
     */
    private void pruneBrokenTargets(Level level) {
        boolean removed = boundTargets.removeIf(bt -> {
            BlockEntity be = level.getBlockEntity(bt.pos());
            UUID current = be == null ? null : be.getData(ModAttachments.BIND_ID);
            // 机器没了（空），或换成了新机器（UUID 不匹配）→ 链接失效
            return current == null || !current.equals(bt.bindId());
        });
        if (removed) {
            targetStorageCache.clear();
            markDirtyAndSync();
            broadcastHighlightUpdate(level); // 让已选该塔的玩家客户端立即去掉残留高亮
            if (DEBUG_LOG) {
                LOGGER.info("[EnergyTower] 塔 {} 已自动断开被破坏机器的绑定", worldPosition);
            }
        }
    }

    /** 把当前高亮数据推送给“已选定本塔”的玩家（用于绑定被自动移除后刷新客户端高亮） */
    private void broadcastHighlightUpdate(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            S2CWrenchHighlightPacket packet =
                    new S2CWrenchHighlightPacket(Optional.of(worldPosition), getBoundTargets());
            for (ServerPlayer sp : serverLevel.players()) {
                if (WrenchSelectionManager.isSelected(sp, level.dimension(), worldPosition)) {
                    PacketDistributor.sendToPlayer(sp, packet);
                }
            }
        }
    }

    public int getBindingCount() {
        return boundTargets.size();
    }

    // ============================== 传输参数 ==============================

    public int getTransferRate() {
        return transferRate;
    }

    /** 修改传输速率（钳制到合法区间） */
    public void setTransferRate(int rate) {
        int clamped = Mth.clamp(rate, 1, MAX_RATE);
        if (clamped != transferRate) {
            transferRate = clamped;
            markDirtyAndSync();
        }
    }

    public boolean isUnlimitedMode() {
        return unlimitedMode;
    }

    public void setUnlimitedMode(boolean unlimited) {
        if (unlimited != unlimitedMode) {
            unlimitedMode = unlimited;
            markDirtyAndSync();
        }
    }

    // ============================== 数据同步（服务端 → 客户端） ==============================

    /** 能量变化回调：标记方块实体存档 */
    private void onEnergyChanged() {
        setChanged();
    }

    /** 参数 / 绑定变化：标记方块实体存档（已无需主动同步客户端渲染，高亮由 S2C 包单独同步） */
    private void markDirtyAndSync() {
        setChanged();
    }

    // ============================== 持久化（退出重进不丢失） ==============================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 能量缓存
        energyStorage.save(tag);
        // 传输参数
        tag.putInt("TransferRate", transferRate);
        tag.putBoolean("UnlimitedMode", unlimitedMode);
        // 绑定清单（含维度标记与方块类型，跨维度绑定在加载时丢弃）
        String dim = level != null ? level.dimension().location().toString() : "";
        ListTag targets = new ListTag();
        for (BoundTarget bt : boundTargets) {
            BlockPos p = bt.pos();
            CompoundTag t = new CompoundTag();
            t.putInt("X", p.getX());
            t.putInt("Y", p.getY());
            t.putInt("Z", p.getZ());
            t.putString("Dim", dim);
            t.putUUID("BindId", bt.bindId());
            targets.add(t);
        }
        tag.put("BoundTargets", targets);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 能量缓存
        energyStorage.load(tag);
        // 传输参数
        transferRate = Mth.clamp(tag.getInt("TransferRate"), 1, MAX_RATE);
        unlimitedMode = tag.getBoolean("UnlimitedMode");
        // 绑定清单
        boundTargets.clear();
        String dim = level != null ? level.dimension().location().toString() : "";
        ListTag targets = tag.getList("BoundTargets", Tag.TAG_COMPOUND);
        for (int i = 0; i < targets.size(); i++) {
            CompoundTag t = targets.getCompound(i);
            String d = t.getString("Dim");
            // 跨维度绑定：加载时静默丢弃，避免无线传输失效
            if (!d.isEmpty() && !d.equals(dim)) {
                continue;
            }
            BlockPos p = new BlockPos(t.getInt("X"), t.getInt("Y"), t.getInt("Z"));
            // 唯一绑定 ID：旧存档没有时，重新生成并标记当前机器（迁移旧链接）
            UUID id = t.hasUUID("BindId") ? t.getUUID("BindId") : null;
            if (id == null) {
                BlockEntity be = level != null ? level.getBlockEntity(p) : null;
                if (be != null) {
                    id = UUID.randomUUID();
                    be.setData(ModAttachments.BIND_ID, id);
                    be.setChanged();
                }
            }
            if (id != null) {
                boundTargets.add(new BoundTarget(p, id));
            }
        }
    }
}
