package net.alshanex.magic_realms.registry;

import dev.xkmc.l2hostility.init.entries.LHRegistrate;
import net.neoforged.neoforge.registries.RegisterEvent;

public class MRRegistrate extends LHRegistrate {

	public MRRegistrate(String modid) {
		super(modid);
	}

	@Override
	protected void onRegister(RegisterEvent event) {
		super.onRegister(event);
	}

}
