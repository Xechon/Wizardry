package com.teamwizardry.wizardry.spells.modules.effects;

import net.minecraft.nbt.NBTTagCompound;
import com.teamwizardry.wizardry.api.modules.Module;
import com.teamwizardry.wizardry.api.modules.attribute.Attribute;
import com.teamwizardry.wizardry.spells.modules.ModuleType;

public class ModuleSaturation extends Module {
    public ModuleSaturation() {
    	attributes.addAttribute(Attribute.POWER);
    }

    @Override
    public ModuleType getType() {
        return ModuleType.EFFECT;
    }

    @Override
    public NBTTagCompound getModuleData() {
        return null;
    }
}