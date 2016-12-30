package com.teamwizardry.wizardry.common.spell.module.effects;

import com.teamwizardry.wizardry.api.Constants;
import com.teamwizardry.wizardry.api.capability.bloods.BloodRegistry;
import com.teamwizardry.wizardry.api.capability.bloods.IBloodType;
import com.teamwizardry.wizardry.api.module.Module;
import com.teamwizardry.wizardry.api.module.attribute.Attribute;
import com.teamwizardry.wizardry.api.spell.IHasAffinity;
import com.teamwizardry.wizardry.api.spell.ModuleType;
import com.teamwizardry.wizardry.api.trackerobject.SpellStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleExplosion extends Module implements IHasAffinity {

	private static final String DAMAGE_TERRAIN = "Damage Terrain";

	private boolean damageTerrain;

	public ModuleExplosion(ItemStack stack) {
		super(stack);
		attributes.addAttribute(Attribute.POWER);
	}

	@Override
	public ModuleType getType() {
		return ModuleType.EFFECT;
	}

	@Override
	public String getDescription() {
		return "Cause an explosion dealing blast damage. More increases the size and stiffness. x64 deals terrain damage.";
	}

	@Override
	public String getDisplayName() {
		return "Explode";
	}

	@Override
	public NBTTagCompound getModuleData() {
		NBTTagCompound compound = super.getModuleData();
		compound.setBoolean(DAMAGE_TERRAIN, damageTerrain);

		compound.setDouble(Constants.Module.POWER, attributes.apply(Attribute.POWER, 1.0));
		compound.setDouble(Constants.Module.MANA, attributes.apply(Attribute.MANA, 10.0));
		compound.setDouble(Constants.Module.BURNOUT, attributes.apply(Attribute.BURNOUT, 10.0));
		return compound;
	}

	public ModuleExplosion setDamageTerrain(boolean canDamageTerrain) {
		damageTerrain = canDamageTerrain;
		return this;
	}

	@Override
	public boolean cast(EntityPlayer player, Entity caster, NBTTagCompound spell, SpellStack stack) {
		List<BlockPos> affectedPositions = new ArrayList<>();
		float power = (float) spell.getDouble(Constants.Module.POWER);
		if (spell.getBoolean(DAMAGE_TERRAIN)) {
			for (int i = -(int) power; i <= power; i++)
				for (int j = -(int) power; j <= power; j++)
					for (int k = -(int) power; k <= power; k++)
						if (((i * i) + (j * j) + (k * k)) < (power * power))
							affectedPositions.add(caster.getPosition().add(i, j, k));
		}
        Explosion explosion = new Explosion(caster.world, player, caster.posX, caster.posY, caster.posZ, power, affectedPositions);
        explosion.doExplosionA();
		explosion.doExplosionB(true);
		return true;
	}

	@Override
	public Map<IBloodType, Integer> getAffinityLevels() {
		Map<IBloodType, Integer> levels = new HashMap<>();
		levels.put(BloodRegistry.PYROBLOOD, 2);
		levels.put(BloodRegistry.ZEPHYRBLOOD, 1);
		return levels;
	}
}
