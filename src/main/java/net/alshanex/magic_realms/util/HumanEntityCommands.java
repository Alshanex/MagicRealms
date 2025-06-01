package net.alshanex.magic_realms.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.alshanex.magic_realms.MagicRealms;
import net.alshanex.magic_realms.data.KillTrackerData;
import net.alshanex.magic_realms.entity.RandomHumanEntity;
import net.alshanex.magic_realms.events.KillTrackingHandler;
import net.alshanex.magic_realms.item.HumanInfoItem;
import net.alshanex.magic_realms.registry.MRDataAttachments;
import net.alshanex.magic_realms.util.humans.CombinedTextureManager;
import net.alshanex.magic_realms.util.humans.EntityClass;
import net.alshanex.magic_realms.util.humans.Gender;
import net.alshanex.magic_realms.util.humans.LayeredTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class HumanEntityCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("human")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("setlevel")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("level", IntegerArgumentType.integer(1, 300))
                                        .executes(HumanEntityCommands::setLevel))))
                .then(Commands.literal("addexp")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .then(Commands.argument("experience", IntegerArgumentType.integer(1, 10000))
                                        .executes(HumanEntityCommands::addExperience))))
                .then(Commands.literal("info")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(HumanEntityCommands::showInfo)))
                .then(Commands.literal("reset")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(HumanEntityCommands::resetLevel)))
                .then(Commands.literal("infoitem")
                        .then(Commands.argument("target", EntityArgument.entity())
                                .executes(HumanEntityCommands::createInfoItem)))
                .then(Commands.literal("createitem")
                        .then(Commands.literal("mage")
                                .then(Commands.literal("male")
                                        .executes(context -> createVirtualInfoItem(context, net.alshanex.magic_realms.util.humans.EntityClass.MAGE, net.alshanex.magic_realms.util.humans.Gender.MALE)))
                                .then(Commands.literal("female")
                                        .executes(context -> createVirtualInfoItem(context, net.alshanex.magic_realms.util.humans.EntityClass.MAGE, net.alshanex.magic_realms.util.humans.Gender.FEMALE))))
                        .then(Commands.literal("warrior")
                                .then(Commands.literal("male")
                                        .executes(context -> createVirtualInfoItem(context, net.alshanex.magic_realms.util.humans.EntityClass.WARRIOR, net.alshanex.magic_realms.util.humans.Gender.MALE)))
                                .then(Commands.literal("female")
                                        .executes(context -> createVirtualInfoItem(context, net.alshanex.magic_realms.util.humans.EntityClass.WARRIOR, net.alshanex.magic_realms.util.humans.Gender.FEMALE))))
                        .then(Commands.literal("rogue")
                                .then(Commands.literal("male")
                                        .executes(context -> createVirtualInfoItem(context, net.alshanex.magic_realms.util.humans.EntityClass.ROGUE, net.alshanex.magic_realms.util.humans.Gender.MALE)))
                                .then(Commands.literal("female")
                                        .executes(context -> createVirtualInfoItem(context, net.alshanex.magic_realms.util.humans.EntityClass.ROGUE, net.alshanex.magic_realms.util.humans.Gender.FEMALE)))))
                .then(Commands.literal("updateitem")
                        .executes(HumanEntityCommands::updateHeldInfoItem)));
    }

    private static int setLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        int level = IntegerArgumentType.getInteger(context, "level");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof RandomHumanEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a RandomHumanEntity!"));
            return 0;
        }

        try {
            KillTrackingHandler.setEntityLevel(humanEntity, level);

            source.sendSuccess(() -> Component.literal(
                    String.format("Set level of %s to %d",
                            humanEntity.getEntityName(), level)), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to set level: " + e.getMessage()));
            return 0;
        }
    }

    private static int addExperience(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        int experience = IntegerArgumentType.getInteger(context, "experience");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof RandomHumanEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a RandomHumanEntity!"));
            return 0;
        }

        try {
            KillTrackingHandler.addExperience(humanEntity, experience);

            String message = String.format("Added %d experience to %s", experience, humanEntity.getEntityName());

            source.sendSuccess(() -> Component.literal(message), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to add experience: " + e.getMessage()));
            return 0;
        }
    }

    private static int showInfo(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof RandomHumanEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a RandomHumanEntity!"));
            return 0;
        }

        try {
            KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);

            source.sendSuccess(() -> Component.literal("=== Human Entity Info ==="), false);
            source.sendSuccess(() -> Component.literal("Name: " + humanEntity.getEntityName()), false);
            source.sendSuccess(() -> Component.literal("UUID: " + humanEntity.getUUID().toString()), false);
            source.sendSuccess(() -> Component.literal("Class: " + humanEntity.getEntityClass().getName()), false);
            source.sendSuccess(() -> Component.literal("Stars: " + humanEntity.getStarLevel()), false);
            source.sendSuccess(() -> Component.literal("Level: " + killData.getCurrentLevel()), false);
            source.sendSuccess(() -> Component.literal("Experience: " + killData.getExperiencePoints()), false);
            source.sendSuccess(() -> Component.literal("Total Kills: " + killData.getTotalKills()), false);
            source.sendSuccess(() -> Component.literal("Exp to Next: " + killData.getExperienceToNextLevel()), false);
            source.sendSuccess(() -> Component.literal("Progress: " + String.format("%.1f%%", killData.getProgressToNextLevel() * 100)), false);

            if (humanEntity.getEntityClass() == net.alshanex.magic_realms.util.humans.EntityClass.ROGUE) {
                source.sendSuccess(() -> Component.literal("Subclass: " + (humanEntity.isArcher() ? "Archer" : "Assassin")), false);
            }

            if (humanEntity.getEntityClass() == net.alshanex.magic_realms.util.humans.EntityClass.MAGE) {
                String schools = humanEntity.getMagicSchools().stream()
                        .map(school -> school.getId().getPath())
                        .collect(java.util.stream.Collectors.joining(", "));
                source.sendSuccess(() -> Component.literal("Magic Schools: " + schools), false);
            }

            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to get info: " + e.getMessage()));
            return 0;
        }
    }

    private static int resetLevel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof RandomHumanEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a RandomHumanEntity!"));
            return 0;
        }

        try {
            KillTrackerData killData = humanEntity.getData(MRDataAttachments.KILL_TRACKER);
            killData.reset();

            // Aplicar nivel 1
            KillTrackingHandler.setEntityLevel(humanEntity, 1);

            source.sendSuccess(() -> Component.literal(
                    String.format("Reset %s to level 1", humanEntity.getEntityName())), true);
            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to reset level: " + e.getMessage()));
            return 0;
        }
    }

    private static int createInfoItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity target = EntityArgument.getEntity(context, "target");
        CommandSourceStack source = context.getSource();

        if (!(target instanceof RandomHumanEntity humanEntity)) {
            source.sendFailure(Component.literal("Target must be a RandomHumanEntity!"));
            return 0;
        }

        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        try {
            ItemStack infoItem = HumanInfoItem.createLinkedItem(humanEntity);

            // Dar el item al jugador
            if (player.getInventory().add(infoItem)) {
                source.sendSuccess(() -> Component.literal(
                        String.format("Created info item for %s", humanEntity.getEntityName())), true);
            } else {
                // Si el inventario está lleno, dropear el item
                player.drop(infoItem, false);
                source.sendSuccess(() -> Component.literal(
                        String.format("Created and dropped info item for %s", humanEntity.getEntityName())), true);
            }

            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to create info item: " + e.getMessage()));
            return 0;
        }
    }

    private static int updateHeldInfoItem(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        try {
            ItemStack heldItem = player.getMainHandItem();

            if (!(heldItem.getItem() instanceof HumanInfoItem)) {
                source.sendFailure(Component.literal("You must be holding a Human Info Item!"));
                return 0;
            }

            // Obtener el UUID de la entidad del item
            EntitySnapshot currentSnapshot = HumanInfoItem.getEntitySnapshot(heldItem);
            if (currentSnapshot == null) {
                source.sendFailure(Component.literal("Item has no entity data!"));
                return 0;
            }

            // Buscar la entidad en el mundo
            RandomHumanEntity entity = findEntityByUUID(player.level(), currentSnapshot.entityUUID);
            if (entity == null) {
                source.sendFailure(Component.literal("Entity not found in world. Cannot update item."));
                return 0;
            }

            // Actualizar el item con los datos actuales de la entidad
            HumanInfoItem.setLinkedEntity(heldItem, entity);

            source.sendSuccess(() -> Component.literal(
                    String.format("Updated info item for %s with current data", entity.getEntityName())), true);

            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to update info item: " + e.getMessage()));
            return 0;
        }
    }

    private static int createVirtualInfoItem(CommandContext<CommandSourceStack> context,
                                             net.alshanex.magic_realms.util.humans.EntityClass entityClass,
                                             net.alshanex.magic_realms.util.humans.Gender gender) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();

        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Only players can use this command!"));
            return 0;
        }

        try {
            // Crear un snapshot virtual con datos predeterminados
            EntitySnapshot virtualSnapshot = createVirtualSnapshot(entityClass, gender);

            // Crear el item con el snapshot virtual
            ItemStack infoItem = HumanInfoItem.createVirtualItem(virtualSnapshot);

            // Dar el item al jugador
            if (player.getInventory().add(infoItem)) {
                source.sendSuccess(() -> Component.literal(
                        String.format("Created virtual info item for %s %s (%s)",
                                gender.getName(), entityClass.getName(), virtualSnapshot.entityName)), true);
            } else {
                // Si el inventario está lleno, dropear el item
                player.drop(infoItem, false);
                source.sendSuccess(() -> Component.literal(
                        String.format("Created and dropped virtual info item for %s %s (%s)",
                                gender.getName(), entityClass.getName(), virtualSnapshot.entityName)), true);
            }

            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to create virtual info item: " + e.getMessage()));
            return 0;
        }
    }

    private static EntitySnapshot createVirtualSnapshot(EntityClass entityClass, Gender gender) {
        UUID virtualUUID = UUID.randomUUID();

        String entityName = net.alshanex.magic_realms.util.humans.AdvancedNameManager.getRandomName(gender);

        Random random = new Random();
        int starLevel = random.nextFloat() < 0.7f ? 1 : (random.nextFloat() < 0.8f ? 2 : 3);

        int currentLevel = 1;

        int totalKills = 0;
        int experiencePoints = 0;

        boolean hasShield = false;
        boolean isArcher = false;
        List<String> magicSchools = new ArrayList<>();

        if (entityClass == EntityClass.WARRIOR) {
            hasShield = random.nextFloat() < 0.25f; // 25% chance
        } else if (entityClass == EntityClass.ROGUE) {
            isArcher = random.nextFloat() < 0.25f; // 25% chance de ser archer
        } else if (entityClass == EntityClass.MAGE) {
            java.util.List<String> availableSchools = java.util.List.of(
                    "irons_spellbooks:fire", "irons_spellbooks:ice", "irons_spellbooks:lightning",
                    "irons_spellbooks:holy", "irons_spellbooks:ender", "irons_spellbooks:blood",
                    "irons_spellbooks:evocation", "irons_spellbooks:nature", "irons_spellbooks:eldritch"
            );

            int schoolCount = random.nextFloat() < 0.65f ? 1 : (random.nextFloat() < 0.85f ? 2 : 3);
            List<String> shuffledSchools = new ArrayList<>(availableSchools);
            Collections.shuffle(shuffledSchools);

            for (int i = 0; i < Math.min(schoolCount, shuffledSchools.size()); i++) {
                magicSchools.add(shuffledSchools.get(i));
            }
        }

        CompoundTag attributes = createVirtualAttributes(entityClass, starLevel, currentLevel, isArcher);

        CompoundTag traits = createVirtualTraits();

        CompoundTag equipment = createVirtualEquipment(entityClass, hasShield, isArcher);

        String texturePath = null;
        try {
            if (FMLEnvironment.dist.isClient()) {
                texturePath = generateAndSaveVirtualTexture(virtualUUID.toString(), gender, entityClass);
            }
        } catch (Exception e) {
            // No es crítico si no se puede generar la textura
        }

        return new EntitySnapshot(
                virtualUUID, entityName, gender, entityClass, starLevel,
                currentLevel, totalKills, experiencePoints, hasShield, isArcher,
                magicSchools, attributes, traits, equipment, virtualUUID.toString(), texturePath
        );
    }

    private static String generateAndSaveVirtualTexture(String uuid, Gender gender, EntityClass entityClass) {
        try {
            ResourceLocation texture =
                    CombinedTextureManager.getCombinedTextureWithHair(
                            uuid, gender, entityClass,
                            LayeredTextureManager.getRandomHairTextureIndex("hair_" + gender.getName())
                    );

            if (texture != null) {
                // La textura ya se guarda automáticamente por CombinedTextureManager
                Path gameDir = Minecraft.getInstance().gameDirectory.toPath();
                Path texturePath = gameDir.resolve("magic_realms_textures").resolve("entity").resolve("human").resolve(uuid + "_complete.png");

                if (Files.exists(texturePath)) {
                    return texturePath.toString();
                }
            }
        } catch (Exception e) {
            MagicRealms.LOGGER.debug("Could not generate virtual texture: {}", e.getMessage());
        }
        return null;
    }

    private static CompoundTag createVirtualAttributes(EntityClass entityClass, int starLevel, int currentLevel, boolean isArcher) {
        CompoundTag attributes = new CompoundTag();
        Random random = new Random();

        double baseHealth = switch (entityClass) {
            case MAGE -> 15.0;
            case WARRIOR -> 20.0;
            case ROGUE -> 10.0;
        };

        attributes.putDouble("health", baseHealth);
        attributes.putDouble("max_health", baseHealth);
        attributes.putDouble("armor", 0.0);
        attributes.putDouble("attack_damage", 3.0);

        // Atributos específicos por clase (nivel 1)
        if (entityClass == EntityClass.MAGE) {
            attributes.putDouble("max_mana", 100.0 + (starLevel * 30));
            attributes.putDouble("mana_regen", 0.1 + (starLevel * 0.05));
            attributes.putDouble("spell_power", 1.0 + (starLevel * 0.2));
            attributes.putDouble("spell_resist", 0.1 + (starLevel * 0.1));
            attributes.putDouble("cooldown_reduction", starLevel * 0.05);
            attributes.putDouble("casting_movespeed", starLevel * 0.05);
            attributes.putDouble("summon_damage", starLevel * 0.1);
        }

        // Atributos de Apothic para nivel 1
        attributes.putDouble("crit_chance", (5 + random.nextInt(15) + (starLevel * 5)) / 100.0);
        attributes.putDouble("crit_damage", 1.5 + (random.nextInt(50) + (starLevel * 10)) / 100.0);
        attributes.putDouble("dodge_chance", (random.nextInt(10) + (entityClass == EntityClass.ROGUE ? 5 : 0)) / 100.0);

        if (entityClass == EntityClass.ROGUE && isArcher) {
            attributes.putDouble("arrow_damage", 1.0 + (random.nextInt(50) + 25) / 100.0);
            attributes.putDouble("arrow_velocity", 1.0 + (random.nextInt(50) + 25) / 100.0);
            attributes.putDouble("draw_speed", 1.0 + (random.nextInt(50) + 25) / 100.0);
        } else if (entityClass == EntityClass.ROGUE) {
            attributes.putDouble("life_steal", random.nextInt(5) / 100.0);
        }

        if (entityClass == EntityClass.WARRIOR) {
            attributes.putDouble("ghost_health", 0.1);
            attributes.putDouble("overheal", random.nextInt(10) / 100.0);
        }

        return attributes;
    }

    private static CompoundTag createVirtualTraits() {
        CompoundTag traits = new CompoundTag();
        ListTag traitList = new ListTag();

        traits.put("trait_list", traitList);
        return traits;
    }

    private static CompoundTag createVirtualEquipment(EntityClass entityClass,
                                                                        boolean hasShield, boolean isArcher) {
        CompoundTag equipment = new CompoundTag();

        // Equipamiento básico basado en la clase
        if (entityClass == EntityClass.WARRIOR) {
            CompoundTag swordTag = new CompoundTag();
            swordTag.putString("id", "minecraft:wooden_sword");
            swordTag.putInt("count", 1);
            equipment.put("main_hand", swordTag);

            if (hasShield) {
                CompoundTag shieldTag = new CompoundTag();
                shieldTag.putString("id", "minecraft:shield");
                shieldTag.putInt("count", 1);
                equipment.put("off_hand", shieldTag);
            }
        } else if (entityClass == net.alshanex.magic_realms.util.humans.EntityClass.ROGUE) {
            if (isArcher) {
                CompoundTag bowTag = new CompoundTag();
                bowTag.putString("id", "minecraft:bow");
                bowTag.putInt("count", 1);
                equipment.put("main_hand", bowTag);
            } else {
                CompoundTag daggerTag = new CompoundTag();
                daggerTag.putString("id", "minecraft:wooden_sword");
                daggerTag.putInt("count", 1);
                equipment.put("main_hand", daggerTag);
            }
        }

        return equipment;
    }

    private static RandomHumanEntity findEntityByUUID(net.minecraft.world.level.Level level, java.util.UUID uuid) {
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(uuid);
            if (entity instanceof RandomHumanEntity humanEntity) {
                return humanEntity;
            }
        }
        return null;
    }
}
