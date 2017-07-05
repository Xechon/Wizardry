package com.teamwizardry.wizardry.common.module.effects;

import com.teamwizardry.librarianlib.features.network.PacketHandler;
import com.teamwizardry.wizardry.api.LightningGenerator;
import com.teamwizardry.wizardry.api.spell.SpellData;
import com.teamwizardry.wizardry.api.spell.attribute.Attributes;
import com.teamwizardry.wizardry.api.spell.module.Module;
import com.teamwizardry.wizardry.api.spell.module.ModuleEffect;
import com.teamwizardry.wizardry.api.spell.module.RegisterModule;
import com.teamwizardry.wizardry.api.util.PosUtils;
import com.teamwizardry.wizardry.api.util.RandUtil;
import com.teamwizardry.wizardry.api.util.RandUtilSeed;
import com.teamwizardry.wizardry.api.util.RayTrace;
import com.teamwizardry.wizardry.common.core.LightningTracker;
import com.teamwizardry.wizardry.common.network.PacketRenderLightningBolt;
import com.teamwizardry.wizardry.init.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.*;

/**
 * Created by LordSaad.
 */
@RegisterModule
public class ModuleEffectLightning extends ModuleEffect {

	@Nonnull
	@Override
	public String getID() {
		return "effect_lightning";
	}

	@Nonnull
	@Override
	public String getReadableName() {
		return "Lightning";
	}

	@Nonnull
	@Override
	public String getDescription() {
		return "Will shock a target, stunning it.";
	}

	@Override
	public boolean run(@Nonnull SpellData spell) {
		World world = spell.world;
		Vec3d target = spell.getData(TARGET_HIT);
		Entity caster = spell.getData(CASTER);
		float yaw = spell.getData(YAW, 0F);
		float pitch = spell.getData(PITCH, 0F);

		if (target == null) return false;

		Vec3d origin = target;
		if (caster != null) {
			float offX = 0.5f * (float) Math.sin(Math.toRadians(-90.0f - yaw));
			float offZ = 0.5f * (float) Math.cos(Math.toRadians(-90.0f - yaw));
			origin = new Vec3d(offX, caster.getEyeHeight(), offZ).add(target);
		}

		double range = getModifier(spell, Attributes.RANGE, 10, 32);
		double strength = getModifier(spell, Attributes.POTENCY, 4, 20) / 2.0;

		if (!tax(this, spell)) return false;

		RayTraceResult traceResult = new RayTrace(world, PosUtils.vecFromRotations(pitch, yaw), target, range).setSkipBlocks(true).setSkipEntities(true).trace();
		if (traceResult == null) return false;

		long seed = RandUtil.nextLong(100, 100000);

		spell.addData(SEED, seed);

		LightningGenerator generator = new LightningGenerator(origin, traceResult.hitVec, new RandUtilSeed(seed));

		ArrayList<Vec3d> points = generator.generate();

		spell.world.playSound(null, new BlockPos(traceResult.hitVec), ModSounds.LIGHTNING, SoundCategory.NEUTRAL, 0.5f, RandUtil.nextFloat(1, 1.5f));
		for (Vec3d point : points) {
			List<Entity> entityList = world.getEntitiesWithinAABBExcludingEntity(caster, new AxisAlignedBB(new BlockPos(point)).contract(0.2, 0.2, 0.2));
			if (!entityList.isEmpty()) {
				for (Entity entity : entityList) {
					LightningTracker.INSTANCE.addEntity(origin, entity, caster, (int) strength);
				}
			}
		}

		return true;
	}

	@Override
	public void runClient(@Nonnull SpellData spell) {
		World world = spell.world;
		float yaw = spell.getData(YAW, 0F);
		float pitch = spell.getData(PITCH, 0F);
		Entity caster = spell.getData(CASTER);
		Vec3d target = spell.getData(TARGET_HIT);
		long seed = spell.getData(SEED, 0L);
		double range = getModifier(spell, Attributes.RANGE, 10, 32);

		if (target == null) return;

		Vec3d origin = target;
		if (caster != null) {
			float offX = 0.5f * (float) Math.sin(Math.toRadians(-90.0f - yaw));
			float offZ = 0.5f * (float) Math.cos(Math.toRadians(-90.0f - yaw));
			origin = new Vec3d(offX, 0, offZ).add(target);
		}

		RayTraceResult traceResult = new RayTrace(world, PosUtils.vecFromRotations(pitch, yaw), target, range).setSkipBlocks(true).setSkipEntities(true).trace();
		if (traceResult == null) return;

		PacketHandler.NETWORK.sendToAllAround(new PacketRenderLightningBolt(origin, traceResult.hitVec, seed),
				new NetworkRegistry.TargetPoint(world.provider.getDimension(), origin.x, origin.y, origin.z, 256));
	}

	@Nonnull
	@Override
	public Module copy() {
		return cloneModule(new ModuleEffectLightning());
	}
}