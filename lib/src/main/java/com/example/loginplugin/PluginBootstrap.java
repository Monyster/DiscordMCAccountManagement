package com.example.loginplugin;

import com.example.loginplugin.dialogs.LoginCodeDialog;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;

public class PluginBootstrap implements io.papermc.paper.plugin.bootstrap.PluginBootstrap {

	@Override
	public void bootstrap(BootstrapContext context) {
		LoginCodeDialog LoginCodeDialog = new LoginCodeDialog(context);
		LoginCodeDialog.register();

		context.getLogger().info("Bootstrap complete______________________________");

//		COMMANDS LIST
//		LoginCommand loginCommand = new LoginCommand();

//		REGISTRATION LOGIC
//		LifecycleEventManager<BootstrapContext> manager = context.getLifecycleManager();

//		manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> {
//			loginCommand.register(event.registrar(), context);
//		});

	}
}