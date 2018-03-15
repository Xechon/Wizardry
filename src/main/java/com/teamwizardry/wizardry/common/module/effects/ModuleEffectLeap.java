package com.teamwizardry.wizardry.common.module.effects;

import com.teamwizardry.wizardry.api.spell.IOverrideCooldown;
import com.teamwizardry.wizardry.api.spell.SpellData;
import com.teamwizardry.wizardry.api.spell.SpellRing;
import com.teamwizardry.wizardry.api.spell.attribute.Attributes;
import com.teamwizardry.wizardry.api.spell.module.ModuleEffect;
import com.teamwizardry.wizardry.api.spell.module.ModuleModifier;
import com.teamwizardry.wizardry.api.spell.module.RegisterModule;
import com.teamwizardry.wizardry.api.util.PosUtils;
import com.teamwizardry.wizardry.client.fx.LibParticles;
import com.teamwizardry.wizardry.common.module.modifiers.ModuleModifierIncreasePotency;
import com.teamwizardry.wizardry.init.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.*;

/**
 * Created by Demoniaque.
 */
@RegisterModule
public class ModuleEffectLeap extends ModuleEffect implements IOverrideCooldown {

	@Nonnull
	@Override
	public String getID() {
		return "effect_leap";
	}

	@Override
	public int getNewCooldown(@Nonnull SpellData data, SpellRing ring) {
		Entity target = data.getData(ENTITY_HIT);
		double strength = ring.getModifier(Attributes.POTENCY, 1, 10);
		if (target == null) return 50;
		if (target.getEntityData().hasKey("jump_count")) {
			int jumpCount = target.getEntityData().getInteger("jump_count");
			if (jumpCount <= 0) {
				target.getEntityData().removeTag("jump_count");
				return 50;
			}
			target.getEntityData().setInteger("jump_count", jumpCount - 1);
			return (int) ((strength - jumpCount)) * 2;
		}
		return 50;
	}

	@Override
	public ModuleModifier[] applicableModifiers() {
		return new ModuleModifier[]{new ModuleModifierIncreasePotency()};
	}

	@Override
	@SuppressWarnings("unused")
	public boolean run(@Nonnull SpellData spell, @Nonnull SpellRing spellRing) {
		float yaw = spell.getData(YAW, 0F);
		float pitch = spell.getData(PITCH, 0F);
		Vec3d pos = spell.getTarget();
		Entity target = spell.getVictim();
		Entity caster = spell.getCaster();

		if (target == null) return false;
		if (!(target instanceof EntityLivingBase)) return true;

		Vec3d lookVec = PosUtils.vecFromRotations(pitch, yaw);

		if (!target.hasNoGravity()) {
			double strength = spellRing.getModifier(Attributes.POTENCY, 1, 64) / 10.0;
			if (!tax(this, spell, spellRing)) return false;

			if (!target.getEntityData().hasKey("jump_count")) {
				target.getEntityData().setInteger("jump_count", (int) strength);
				target.getEntityData().setInteger("jump_timer", 200);
			}

			target.motionX += lookVec.x;
			target.motionY += 0.65;
			target.motionZ += lookVec.z;

			target.velocityChanged = true;
			target.fallDistance /= spellRing.getModifier(Attributes.POTENCY, 2, 10);

			if (target instanceof EntityPlayerMP)
				((EntityPlayerMP) target).connection.sendPacket(new SPacketEntityVelocity(target));
			spell.world.playSound(null, target.getPosition(), ModSounds.FLY, SoundCategory.NEUTRAL, 1, 1);
		}
		return true;
	}

	@Override
	@SuppressWarnings("unused")
	@SideOnly(Side.CLIENT)
	public void render(@Nonnull SpellData spell, @Nonnull SpellRing spellRing) {
		Entity caster = spell.getCaster();
		Vec3d position = spell.getTarget();
		Entity entityHit = spell.getVictim();

		if (position == null) return;
		if (entityHit == null) return;

		if (!entityHit.hasNoGravity()) {
			Vec3d normal = new Vec3d(entityHit.motionX, entityHit.motionY, entityHit.motionZ).normalize().scale(1 / 2.0);

			LibParticles.AIR_THROTTLE(spell.world, position, normal, getPrimaryColor(), getSecondaryColor(), 0.5);

		}
	}

	@SubscribeEvent
	public void tickEntity(LivingEvent.LivingUpdateEvent event) {
		if (event.getEntityLiving().getEntityData().hasKey("jump_timer")) {
			int x = event.getEntityLiving().getEntityData().getInteger("jump_timer");

			if (event.getEntityLiving().collidedVertically) {
				event.getEntityLiving().getEntityData().removeTag("jump_timer");
				event.getEntityLiving().getEntityData().removeTag("jump_count");
				return;
			}

			if (x <= 0) {
				event.getEntityLiving().getEntityData().removeTag("jump_timer");
				event.getEntityLiving().getEntityData().removeTag("jump_count");
			} else event.getEntityLiving().getEntityData().setInteger("jump_timer", x - 1);
		}
	}
}
