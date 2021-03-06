package com.teamwizardry.wizardry.init;

import com.teamwizardry.librarianlib.features.base.ModSoundEvent;

/**
 * Created by Demoniaque on 6/29/2016.
 */
public class ModSounds {
	public static ModSoundEvent GLASS_BREAK;
	public static ModSoundEvent FIZZING_LOOP;
	public static ModSoundEvent FRYING_SIZZLE;
	public static ModSoundEvent BUBBLING;
	public static ModSoundEvent HARP1;
	public static ModSoundEvent HARP2;
	public static ModSoundEvent BELL;
	public static ModSoundEvent HALLOWED_SPIRIT;
	public static ModSoundEvent EXPLOSION_BOOM;
	public static ModSoundEvent PROJECTILE_LAUNCH;
	public static ModSoundEvent BASS_BOOM;
	public static ModSoundEvent CHAINY_ZAP;
	public static ModSoundEvent CHORUS_GOOD;
	public static ModSoundEvent COLD_WIND;
	public static ModSoundEvent ELECTRIC_BLAST;
	public static ModSoundEvent ETHEREAL_PASS_BY;
	public static ModSoundEvent FAIRY;
	public static ModSoundEvent FIRE;
	public static ModSoundEvent FIREBALL;
	public static ModSoundEvent FLY;
	public static ModSoundEvent FROST_FORM;
	public static ModSoundEvent HEAL;
	public static ModSoundEvent LIGHTNING;
	public static ModSoundEvent SLOW_MOTION_IN;
	public static ModSoundEvent SLOW_MOTION_OUT;
	public static ModSoundEvent SMOKE_BLAST;
	public static ModSoundEvent TELEPORT;
	public static ModSoundEvent THUNDERBLAST;
	public static ModSoundEvent WIND;
	public static ModSoundEvent ZAP;
	public static ModSoundEvent ELECTRIC_WHITE_NOISE;
	public static ModSoundEvent SPARKLE;
	public static ModSoundEvent POP;
	public static ModSoundEvent BELL_TING;
	public static ModSoundEvent BUTTON_CLICK_IN;
	public static ModSoundEvent BUTTON_CLICK_OUT;
	public static ModSoundEvent ETHEREAL;
	public static ModSoundEvent WHOOSH;
	public static ModSoundEvent WING_FLAP;
	public static ModSoundEvent ZOOM;
	public static ModSoundEvent GOOD_ETHEREAL_CHILLS;
	public static ModSoundEvent SCRIBBLING;
	public static ModSoundEvent SPELL_FAIL;
	public static ModSoundEvent GAS_LEAK;
	public static ModSoundEvent GRACE;
	public static ModSoundEvent SOUND_BOMB;
	public static ModSoundEvent FIREWORK;
	public static ModSoundEvent MARBLE_EXPLOSION;
	public static ModSoundEvent SLIME_SQUISHING;

	public static void init() {
		SPELL_FAIL = new ModSoundEvent("spell_fail");
		SCRIBBLING = new ModSoundEvent("scribbling");
		GOOD_ETHEREAL_CHILLS = new ModSoundEvent("good_ethereal_chills");
		ZOOM = new ModSoundEvent("zoom");
		WING_FLAP = new ModSoundEvent("wing_flap");
		WHOOSH = new ModSoundEvent("whoosh");
		ETHEREAL = new ModSoundEvent("ethereal");
		BUTTON_CLICK_OUT = new ModSoundEvent("button_click_out");
		BUTTON_CLICK_IN = new ModSoundEvent("button_click_in");
		BELL_TING = new ModSoundEvent("bell_ting");
		POP = new ModSoundEvent("pop");
		GLASS_BREAK = new ModSoundEvent("glassbreak");
		FIZZING_LOOP = new ModSoundEvent("fizzingloop");
		FRYING_SIZZLE = new ModSoundEvent("firesizzleloop");
		HARP1 = new ModSoundEvent("harp1");
		HARP2 = new ModSoundEvent("harp2");
		BELL = new ModSoundEvent("bell");
		BUBBLING = new ModSoundEvent("bubbling");
		HALLOWED_SPIRIT = new ModSoundEvent("hallowed_spirit_shriek");
		EXPLOSION_BOOM = new ModSoundEvent("expl_boom");
		PROJECTILE_LAUNCH = new ModSoundEvent("proj_launch");
		BASS_BOOM = new ModSoundEvent("bass_boom");
		CHAINY_ZAP = new ModSoundEvent("chainy_zap");
		CHORUS_GOOD = new ModSoundEvent("chorus_good");
		COLD_WIND = new ModSoundEvent("cold_wind");
		ELECTRIC_BLAST = new ModSoundEvent("electric_blast");
		ETHEREAL_PASS_BY = new ModSoundEvent("ethereal_pass_by");
		FAIRY = new ModSoundEvent("fairy_1");
		FIRE = new ModSoundEvent("fire");
		FIREBALL = new ModSoundEvent("fireball");
		FLY = new ModSoundEvent("fly");
		FROST_FORM = new ModSoundEvent("frost_form");
		HEAL = new ModSoundEvent("heal");
		LIGHTNING = new ModSoundEvent("lightning");
		SLOW_MOTION_IN = new ModSoundEvent("slow_motion_in");
		SLOW_MOTION_OUT = new ModSoundEvent("slow_motion_out");
		SMOKE_BLAST = new ModSoundEvent("smoke_blast");
		TELEPORT = new ModSoundEvent("teleport");
		THUNDERBLAST = new ModSoundEvent("thunder_blast");
		WIND = new ModSoundEvent("wind");
		ZAP = new ModSoundEvent("zap");
		ELECTRIC_WHITE_NOISE = new ModSoundEvent("electric_white_noise");
		SPARKLE = new ModSoundEvent("sparkle");
		GAS_LEAK = new ModSoundEvent("gas_leak");
		GRACE = new ModSoundEvent("grace");
		SOUND_BOMB = new ModSoundEvent("sound_bomb");
		FIREWORK = new ModSoundEvent("firework");
		MARBLE_EXPLOSION = new ModSoundEvent("marble_explosion");
		SLIME_SQUISHING = new ModSoundEvent("slime_squishing");
	}

}
