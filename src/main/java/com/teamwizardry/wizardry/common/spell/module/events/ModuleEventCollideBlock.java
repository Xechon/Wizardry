package com.teamwizardry.wizardry.common.spell.module.events;

import com.teamwizardry.wizardry.api.spell.Module;
import com.teamwizardry.wizardry.api.spell.ModuleType;
import com.teamwizardry.wizardry.api.spell.SpellStack;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Created by LordSaad.
 */
public class ModuleEventCollideBlock extends Module {

    public ModuleEventCollideBlock() {
    }

    @NotNull
    @Override
    public ItemStack getRequiredStack() {
        return new ItemStack(Items.COAL);
    }

    @NotNull
    @Override
    public ModuleType getModuleType() {
        return ModuleType.EVENT;
    }

    @NotNull
    @Override
    public String getID() {
        return "on_collide_block";
    }

    @NotNull
    @Override
    public String getReadableName() {
        return "On Collide Block";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Triggered when the spell collides with a block";
    }

    @NotNull
    @Override
    public Set<Module> getCompatibleModifierModules() {
        return super.getCompatibleModifierModules();
    }

    @Override
    public boolean run(@NotNull World world, @Nullable EntityLivingBase caster, @NotNull SpellStack spellStack) {
        return super.run(world, caster, spellStack);
    }

    @NotNull
    @Override
    public Module copy() {
        ModuleEventCollideBlock clone = new ModuleEventCollideBlock();
        clone.modifierModules = modifierModules;
        clone.extraModifiers = extraModifiers;
        clone.children = children;
        return clone;
    }
}
