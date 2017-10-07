package com.teamwizardry.wizardry.common.entity.angel;

import com.teamwizardry.librarianlib.features.saving.AbstractSaveHandler;
import com.teamwizardry.librarianlib.features.saving.SaveInPlace;
import com.teamwizardry.wizardry.api.arena.Arena;
import com.teamwizardry.wizardry.api.arena.ArenaManager;
import com.teamwizardry.wizardry.api.arena.ZachTimeManager;
import com.teamwizardry.wizardry.common.entity.EntityZachrielCorruption;
import com.teamwizardry.wizardry.init.ModItems;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Created by LordSaad.
 */
@SaveInPlace
public class EntityZachriel extends EntityAngel {

	public boolean saveTime = false;
	public boolean reverseTime = false;

	public EntityZachriel(World worldIn) {
		super(worldIn);
		setCustomNameTag("Zachriel");
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		ZachTimeManager manager = null;
		for (ZachTimeManager manager1 : ArenaManager.INSTANCE.zachTimeManagers) {
			if (manager1.getEntityZachriel().getEntityId() == getEntityId()) {
				manager = manager1;
			}
		}

		if (manager == null) return false;

		ZachTimeManager.BasicPalette palette = manager.getPalette();
		for (BlockPos pos : manager.getTrackedBlocks()) {
			HashMap<Long, IBlockState> states = manager.getBlocksAtPos(pos, palette);
			Thread thread = new Thread(() -> {
				for (Long time : states.keySet()) {
					IBlockState state = states.get(time);
					world.setBlockState(pos, state);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
			thread.start();
		}

		return false;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();

		fallDistance = 0;

		// BATTLE
		{
			//if (!isBeingBattled()) return;

			if (!world.isRemote) {
				List<Entity> entityList = world.getEntitiesWithinAABBExcludingEntity(this, new AxisAlignedBB(getPosition()).grow(2));
				boolean shouldSpawnMoreCorruption = true;
				if (!entityList.isEmpty())
					for (Entity entity : entityList) {
						if (entity instanceof EntityZachrielCorruption) {
							shouldSpawnMoreCorruption = false;
						}
					}
				if (shouldSpawnMoreCorruption) {
					EntityZachrielCorruption corruption = new EntityZachrielCorruption(world);
					corruption.setPosition(posX, posY, posZ);
					world.spawnEntity(corruption);
				}

			}
		}
	}

	@Override
	protected boolean processInteract(EntityPlayer player, EnumHand hand) {
		if (player.getHeldItemMainhand().getItem() == ModItems.MAGIC_WAND) {
			HashSet<UUID> players = new HashSet<>();
			players.add(player.getUniqueID());
			ArenaManager.INSTANCE.addArena(new Arena(player.world.provider.getDimension(), getPosition(), 10, 10, getEntityId(), players));
		}
		return super.processInteract(player, hand);
	}

	@Override
	public void dropLoot(boolean wasRecentlyHit, int lootingModifier, @Nonnull DamageSource source) {
		super.dropLoot(wasRecentlyHit, lootingModifier, source);
	}

	@Override
	public void writeCustomNBT(@NotNull NBTTagCompound compound) {
		super.writeCustomNBT(compound);
		compound.setTag("save", AbstractSaveHandler.writeAutoNBT(this, true));
	}

	@Override
	public void readCustomNBT(@NotNull NBTTagCompound compound) {
		super.readCustomNBT(compound);
		AbstractSaveHandler.readAutoNBT(this, compound.getCompoundTag("save"), true);
	}
}
