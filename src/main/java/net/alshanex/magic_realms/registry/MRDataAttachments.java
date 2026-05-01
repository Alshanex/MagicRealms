package net.alshanex.magic_realms.registry;

import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.*;
import net.neoforged.bus.api.IEventBus;
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
                    .sync(KillTrackerData.STREAM_CODEC)
                    .build()
    );

    public static final Supplier<AttachmentType<ContractData>> CONTRACT_DATA = ATTACHMENT_TYPES.register(
            "contract_data", () -> AttachmentType.builder(() -> new ContractData()).serialize(new ContractData.Serializer()).build()
    );

    public static final Supplier<AttachmentType<ChairSittingData>> CHAIR_SITTING = ATTACHMENT_TYPES.register(
            "chair_sitting",
            () -> AttachmentType.builder(ChairSittingData::new)
                    .serialize(ChairSittingData.CODEC)
                    .build()
    );

    public static final Supplier<AttachmentType<PatrolData>> PATROL = ATTACHMENT_TYPES.register(
            "patrol",
            () -> AttachmentType.builder(PatrolData::new)
                    .serialize(PatrolData.CODEC)
                    .build()
    );

    public static final Supplier<AttachmentType<FearData>> FEAR = ATTACHMENT_TYPES.register(
            "fear",
            () -> AttachmentType.builder(FearData::new)
                    .serialize(new FearData.Serializer())
                    .build()
    );

    public static final Supplier<AttachmentType<MercenaryIdentity>> IDENTITY = ATTACHMENT_TYPES.register(
            "identity",
            () -> AttachmentType.builder(MercenaryIdentity::new)
                    .serialize(new MercenaryIdentity.Serializer())
                    .build()
    );

    public static final Supplier<AttachmentType<PersonalityData>> PERSONALITY = ATTACHMENT_TYPES.register(
            "personality",
            () -> AttachmentType.builder(PersonalityData::new)
                    .serialize(new PersonalityData.Serializer())
                    .sync(PersonalityData.STREAM_CODEC)
                    .build()
    );

    public static final Supplier<AttachmentType<AssignedFixedPersonalitiesData>> ASSIGNED_FIXED_PERSONALITIES =
            ATTACHMENT_TYPES.register(
                    "assigned_fixed_personalities",
                    () -> AttachmentType.builder(AssignedFixedPersonalitiesData::new)
                            .serialize(new AssignedFixedPersonalitiesData.Serializer())
                            .build()
            );

    public static final Supplier<AttachmentType<FloatingArrowData>> FLOATING_ARROW =
            ATTACHMENT_TYPES.register(
                    "floating_arrow",
                    () -> AttachmentType.builder(FloatingArrowData::new)
                            .build()
            );

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
