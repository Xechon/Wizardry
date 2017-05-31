package com.teamwizardry.wizardry.common.core;

import com.teamwizardry.librarianlib.features.helpers.ItemNBTHelper;
import com.teamwizardry.wizardry.Wizardry;
import com.teamwizardry.wizardry.api.Constants.MISC;
import com.teamwizardry.wizardry.api.capability.CapManager;
import com.teamwizardry.wizardry.api.spell.IContinousSpell;
import com.teamwizardry.wizardry.api.spell.Module;
import com.teamwizardry.wizardry.api.spell.SpellCastEvent;
import com.teamwizardry.wizardry.api.spell.SpellData;
import com.teamwizardry.wizardry.api.util.PosUtils;
import com.teamwizardry.wizardry.api.util.RandUtil;
import com.teamwizardry.wizardry.api.util.TeleportUtil;
import com.teamwizardry.wizardry.common.achievement.Achievements;
import com.teamwizardry.wizardry.common.entity.EntityDevilDust;
import com.teamwizardry.wizardry.common.entity.EntityFairy;
import com.teamwizardry.wizardry.init.ModItems;
import com.teamwizardry.wizardry.init.ModPotions;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.TextureStitchEvent.Pre;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerFlyableFallEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.WorldTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EventHandler {

	private final ArrayList<UUID> fallResetUUIDs = new ArrayList<>();

	@SubscribeEvent
	public void onTextureStitchEvent(Pre event) {
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, MISC.SPARKLE));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, MISC.SPARKLE_BLURRED));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, "particles/hexagon"));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, "particles/octagon"));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, "particles/hexagon_blur_1"));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, "particles/hexagon_blur_2"));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, "particles/hexagon_blur_3"));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, "particles/octagon_blur_1"));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, "particles/octagon_blur_2"));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, "particles/octagon_blur_3"));
		event.getMap().registerSprite(new ResourceLocation(Wizardry.MODID, "particles/sprite_sheet"));
	}

	@SubscribeEvent
	public void redstoneBornEvent(EntityJoinWorldEvent event) {
		if (event.getEntity() instanceof EntityItem) {
			EntityItem item = (EntityItem) event.getEntity();
			if (item.getEntityItem().getItem() == Items.REDSTONE)
				event.getWorld().spawnEntity(new EntityDevilDust(event.getWorld(), item));
		}
	}

	@SubscribeEvent
	public void tickEvent(WorldTickEvent event) {
		if (event.phase != Phase.START) {
			if (!fallResetUUIDs.isEmpty())
				event.world.playerEntities.stream().filter(entity -> fallResetUUIDs.contains(entity.getUniqueID())).forEach(entity -> entity.fallDistance = -255);
			fallResetUUIDs.clear();
		}
	}

	@SubscribeEvent
	public void onFallDamage(LivingHurtEvent event) {
		if (!(event.getEntity() instanceof EntityPlayer)) return;
		if (event.getSource() == EntityDamageSource.OUT_OF_WORLD) {
			EntityPlayer player = ((EntityPlayer) event.getEntityLiving());
			BlockPos spawn = player.isSpawnForced(0) ? player.getBedLocation(0) : player.world.getSpawnPoint().add(player.world.rand.nextGaussian() * 16, 0, player.world.rand.nextGaussian() * 16);
			BlockPos teleportTo = spawn.add(0, 300 - spawn.getY(), 0);
			TeleportUtil.teleportToDimension((EntityPlayer) event.getEntity(), 0, teleportTo.getX(), teleportTo.getY(), teleportTo.getZ());
			event.getEntity().fallDistance = -500;
			event.setCanceled(true);
		}
		if (event.getEntity().getEntityWorld().provider.getDimension() == Wizardry.underWorld.getId()) {
			event.getEntity().fallDistance = 0;
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onFall(LivingFallEvent event) {
		if (!(event.getEntity() instanceof EntityPlayer)) return;
		if (event.getEntity().getEntityWorld().provider.getDimension() != Wizardry.underWorld.getId()) {
			if (event.getEntity().fallDistance >= 250) {
				BlockPos location = event.getEntity().getPosition();
				BlockPos bedrock = PosUtils.checkNeighbor(event.getEntity().getEntityWorld(), location, Blocks.BEDROCK);
				if (bedrock != null) {
					if (event.getEntity().getEntityWorld().getBlockState(bedrock).getBlock() == Blocks.BEDROCK) {
						TeleportUtil.teleportToDimension((EntityPlayer) event.getEntity(), Wizardry.underWorld.getId(), 0, 300, 0);
						((EntityPlayer) event.getEntity()).addPotionEffect(new PotionEffect(ModPotions.NULLIFY_GRAVITY, 100, 1, false, false));
						fallResetUUIDs.add(event.getEntity().getUniqueID());
						((EntityPlayer) event.getEntity()).addStat(Achievements.CRUNCH);
						event.setCanceled(true);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void onFlyFall(PlayerFlyableFallEvent event) {
		if (event.getEntityPlayer().getEntityWorld().provider.getDimension() != Wizardry.underWorld.getId()) {
			if (event.getEntityPlayer().fallDistance >= 250) {
				BlockPos location = event.getEntityPlayer().getPosition();
				BlockPos bedrock = PosUtils.checkNeighbor(event.getEntity().getEntityWorld(), location, Blocks.BEDROCK);
				if (bedrock != null) {
					if (event.getEntity().getEntityWorld().getBlockState(bedrock).getBlock() == Blocks.BEDROCK) {
						TeleportUtil.teleportToDimension(event.getEntityPlayer(), Wizardry.underWorld.getId(), 0, 300, 0);
						((EntityPlayer) event.getEntity()).addPotionEffect(new PotionEffect(ModPotions.NULLIFY_GRAVITY, 100, 1, false, false));
						fallResetUUIDs.add(event.getEntityPlayer().getUniqueID());
						event.getEntityPlayer().addStat(Achievements.CRUNCH);
					}
				}
			}
		}
	}

	@SubscribeEvent
	public void capTick(TickEvent.PlayerTickEvent event) {
		CapManager manager = new CapManager(event.player);
		manager.addMana(manager.getMaxMana() / 1000);
		manager.removeBurnout(manager.getMaxBurnout() / 1000);

		ItemStack cape = event.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST);
		if (manager.getMaxMana() < 100)
			manager.setMaxMana(100);
		if (manager.getMaxBurnout() < 100)
			manager.setMaxBurnout(100);

		if (!cape.isEmpty() && cape.getItem() == ModItems.CAPE) {
			double x = ItemNBTHelper.getInt(cape, "time", 0) / 1000.0;
			double buffer = (1 - (Math.exp(-x))) * 5000;
			if (buffer < 100) return;
			manager.setMaxMana(buffer);
			manager.setMaxBurnout(buffer);
		}
//		Minecraft.getMinecraft().player.sendChatMessage(CapManager.getMaxMana(event.player) + "");
	}

	@SubscribeEvent
	public void fairyAmbush(SpellCastEvent event) {
		Entity caster = event.spell.getData(SpellData.DefaultKeys.CASTER);
		int chance = 5;
		for (Module module : event.module.getAllChildModules())
			if (module instanceof IContinousSpell) {
				chance = 100;
				break;
			}
		if (caster != null) {
			List<EntityFairy> fairyList = event.spell.world.getEntitiesWithinAABB(EntityFairy.class, new AxisAlignedBB(caster.getPosition()).expand(64, 64, 64));
			for (EntityFairy fairy : fairyList) {
				if (RandUtil.nextInt(chance) == 0) fairy.ambush = true;
			}
		}
	}
}
