package net.alshanex.magic_realms.data;

import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import net.alshanex.magic_realms.MagicRealms;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.common.util.INBTSerializable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class MercenaryIdentity implements INBTSerializable<CompoundTag> {
    private int starLevel = 1;
    private final List<SchoolType> magicSchools = new ArrayList<>();
    private final List<AbstractSpell> persistedSpells = new ArrayList<>();
    private boolean spellsGenerated = false;
    private boolean goalsInitialized = false;

    public MercenaryIdentity() {}

    public int getStarLevel() { return starLevel; }
    public void setStarLevel(int level) {
        if (level >= 1 && level <= 3) this.starLevel = level;
    }

    public List<SchoolType> getMagicSchools() { return new ArrayList<>(magicSchools); }
    public void setMagicSchools(List<SchoolType> schools) {
        this.magicSchools.clear();
        this.magicSchools.addAll(schools);
    }
    public boolean hasSchool(SchoolType school) { return magicSchools.contains(school); }

    public List<AbstractSpell> getPersistedSpells() { return new ArrayList<>(persistedSpells); }
    public void setPersistedSpells(List<AbstractSpell> spells) {
        this.persistedSpells.clear();
        this.persistedSpells.addAll(spells);
    }
    public void clearPersistedSpells() { this.persistedSpells.clear(); }

    public boolean areSpellsGenerated() { return spellsGenerated; }
    public void setSpellsGenerated(boolean generated) { this.spellsGenerated = generated; }

    public boolean areGoalsInitialized() { return goalsInitialized; }
    public void setGoalsInitialized(boolean initialized) { this.goalsInitialized = initialized; }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("star_level", starLevel);
        tag.putBoolean("spells_generated", spellsGenerated);
        tag.putBoolean("goals_initialized", goalsInitialized);

        ListTag schoolsTag = new ListTag();
        for (SchoolType school : magicSchools) {
            schoolsTag.add(StringTag.valueOf(school.getId().toString()));
        }
        tag.put("magic_schools", schoolsTag);

        ListTag spellsTag = new ListTag();
        for (AbstractSpell spell : persistedSpells) {
            spellsTag.add(StringTag.valueOf(spell.getSpellResource().toString()));
        }
        tag.put("persisted_spells", spellsTag);

        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        this.starLevel = tag.contains("star_level") ? tag.getInt("star_level") : 1;
        this.spellsGenerated = tag.getBoolean("spells_generated");
        this.goalsInitialized = tag.getBoolean("goals_initialized");

        this.magicSchools.clear();
        if (tag.contains("magic_schools")) {
            ListTag schoolsTag = tag.getList("magic_schools", Tag.TAG_STRING);
            for (int i = 0; i < schoolsTag.size(); i++) {
                try {
                    ResourceLocation loc = ResourceLocation.parse(schoolsTag.getString(i));
                    SchoolType school = SchoolRegistry.getSchool(loc);
                    if (school != null) this.magicSchools.add(school);
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Failed to parse school from attachment: {}", schoolsTag.getString(i), e);
                }
            }
        }

        this.persistedSpells.clear();
        if (tag.contains("persisted_spells")) {
            ListTag spellsTag = tag.getList("persisted_spells", Tag.TAG_STRING);
            for (int i = 0; i < spellsTag.size(); i++) {
                try {
                    ResourceLocation loc = ResourceLocation.parse(spellsTag.getString(i));
                    AbstractSpell spell = SpellRegistry.getSpell(loc);
                    if (spell != null) this.persistedSpells.add(spell);
                    else MagicRealms.LOGGER.warn("Failed to find spell: {}", loc);
                } catch (Exception e) {
                    MagicRealms.LOGGER.warn("Failed to parse spell from attachment: {}", spellsTag.getString(i), e);
                }
            }
        }
    }

    public static class Serializer implements IAttachmentSerializer<CompoundTag, MercenaryIdentity> {
        @Override
        public MercenaryIdentity read(IAttachmentHolder holder, CompoundTag tag, HolderLookup.Provider provider) {
            MercenaryIdentity data = new MercenaryIdentity();
            data.deserializeNBT(provider, tag);
            return data;
        }

        @Nullable
        @Override
        public CompoundTag write(MercenaryIdentity attachment, HolderLookup.Provider provider) {
            return attachment.serializeNBT(provider);
        }
    }
}
