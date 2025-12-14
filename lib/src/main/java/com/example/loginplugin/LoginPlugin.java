package com.example.loginplugin;

import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class LoginPlugin extends JavaPlugin {

	private static LoginPlugin instance;
	private LoginManager loginManager;
	private Logger logger;

	@Override
	public void onLoad() {
		instance = this;
	}

	@Override
	public void onEnable() {
		instance = this;
		this.logger = getLogger();
		this.loginManager = new LoginManager(logger);

		// Register listener
		getServer().getPluginManager().registerEvents(new ServerJoinListener(instance), this);

		logger.info("LoginPlugin enabled!");
	}

	@Override
	public void onDisable() {
		// Close database connection properly
		if (loginManager != null) {
			loginManager.closeDatabase();
		}
		logger.info("LoginPlugin disabled!");
	}

	public LoginManager getLoginManager() {
		return loginManager;
	}

	public Logger getPluginLogger() {
		return logger;
	}

	public static LoginPlugin getInstance() {
		return instance;
	}
}
