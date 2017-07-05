package com.teamwizardry.wizardry.common.module.events;

import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.BLOCK_HIT;
import static com.teamwizardry.wizardry.api.spell.SpellData.DefaultKeys.ENTITY_HIT;

import javax.annotation.Nonnull;

import com.teamwizardry.wizardry.api.spell.SpellData;
import com.teamwizardry.wizardry.api.spell.module.Module;
import com.teamwizardry.wizardry.api.spell.module.ModuleEvent;
import com.teamwizardry.wizardry.api.spell.module.RegisterModule;

import net.minecraft.entity.Entity;

/**
 * Created by LordSaad.
 */
@RegisterModule
public class ModuleEventCollideEntity extends ModuleEvent {

	@Nonnull
	@Override
	public String getID() {
		return "event_collide_entity";
	}

	@Nonnull
	@Override
	public String getReadableName() {
		return "On Collide Entity";
	}

	@Nonnull
	@Override
	public String getDescription() {
		return "Triggered when the spell collides with an entity";
	}

	@Override
	public boolean run(@Nonnull SpellData spell) {
		Entity entity = spell.getData(ENTITY_HIT);
		spell.removeData(BLOCK_HIT);
		return entity != null && nextModule != null && nextModule.run(spell);
	}

	@Override
	public void runClient(@Nonnull SpellData spell) {

	}

	@Nonnull
	@Override
	public Module copy() {
		return cloneModule(new ModuleEventCollideEntity());
	}
}