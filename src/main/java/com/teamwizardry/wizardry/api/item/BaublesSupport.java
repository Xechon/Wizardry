package com.teamwizardry.wizardry.api.item;

import baubles.api.BaubleType;
import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Optional;

public final class BaublesSupport {
	private BaublesSupport() {
	}

	public static ItemStack getItem(EntityLivingBase entity, Item item) {
		for (ItemStack stack : getArmor(entity)) {
			if (stack.getItem() == item) {
				return stack;
			}
		}
		return ItemStack.EMPTY;
	}

	public static Iterable<ItemStack> getArmor(EntityLivingBase entity) {
		return ArmorHolder.ACCESSOR.get(entity);
	}

	private static final class ArmorHolder {
		private static final FallbackArmorAccessor ACCESSOR = new ArmorAccessor();
	}

	private static class FallbackArmorAccessor {
		public Iterable<ItemStack> get(EntityLivingBase entity) {
			return entity.getArmorInventoryList();
		}
	}

	private static final class ArmorAccessor extends FallbackArmorAccessor {
		@Override
		@Optional.Method(modid = "baubles")
		public Iterable<ItemStack> get(EntityLivingBase entity) {
			if (!(entity instanceof EntityPlayer)) return get(entity);
			if (BaublesApi.getBaublesHandler((EntityPlayer) entity) == null) return get(entity);

			ImmutableList.Builder<ItemStack> stacks = ImmutableList.builder();
			IBaublesItemHandler inv = BaublesApi.getBaublesHandler((EntityPlayer) entity);
			for (BaubleType type : BaubleType.values())
				for (int slot : type.getValidSlots()) {
					stacks.add(inv.getStackInSlot(slot));
				}
			return stacks/*.addAll(super.get(entity))*/.build();
		}
	}
}