package com.energytower;

import com.mojang.serialization.Codec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.UUID;

/**
 * 附件类型注册（NeoForge 1.21.1 附件系统）。
 * <p>
 * {@link #BIND_ID}：给“被绑定的机器方块实体”打一个唯一 UUID。
 * 用于精确识别“同一台机器”——机器被拆后该附件随之消失，即使原位再放同类型机器，
 * 新机器也没有这个 UUID，从而能被识别为不同实例并自动断开旧链接。
 */
public final class ModAttachments {

    private ModAttachments() {
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, EnergyTower.MODID);

    /** 机器方块实体上的绑定唯一 ID（可持久化，随方块实体 NBT 保存） */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<UUID>> BIND_ID =
            ATTACHMENTS.register("bind_id",
                    () -> AttachmentType.<UUID>builder(() -> null)
                            .serialize(Codec.STRING.xmap(s -> UUID.fromString(s), uuid -> uuid.toString()))
                            .build());

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }
}
