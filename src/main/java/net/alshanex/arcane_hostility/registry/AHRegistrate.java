package net.alshanex.arcane_hostility.registry;

import dev.xkmc.l2hostility.init.entries.LHRegistrate;
import net.neoforged.neoforge.registries.RegisterEvent;

public class AHRegistrate extends LHRegistrate {

	public AHRegistrate(String modid) {
		super(modid);
	}

	@Override
	protected void onRegister(RegisterEvent event) {
		super.onRegister(event);
	}

}
