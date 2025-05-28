package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class MRDataAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MagicRealms.MODID);

    public static final Supplier<AttachmentType<KillTrackerData>> KILL_TRACKER = ATTACHMENT_TYPES.register(
            "kill_tracker",
            () -> AttachmentType.builder(KillTrackerData::new)
                    .serialize(KillTrackerData.CODEC)
                    .build()
    );

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
