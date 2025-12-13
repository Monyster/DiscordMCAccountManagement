package com.example.loginplugin;

import net.kyori.adventure.text.TextComponent;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LoginCommand {

	public LoginCommand() {
	}

	public void register(Commands commands, BootstrapContext context) {
		LiteralCommandNode<CommandSourceStack> loginCommand = buildCommand();
		commands.register(loginCommand, "Command for login");
		context.getLogger().info("Command /login registered successfully!");
	}

	/**
	 * Builds /login command.
	 */
	private LiteralCommandNode<CommandSourceStack> buildCommand() {
		return Commands.literal("login").executes(ctx -> {
			ctx.getSource().getSender().sendMessage(Component.text("loginUsageMsg", NamedTextColor.RED));
			return 0;
		}).then(Commands.argument("code", StringArgumentType.word())
				.executes(ctx -> executeLogin(ctx.getSource(), StringArgumentType.getString(ctx, "code")))).build();
	}

	/**
	 * Handles logic when a code is provided.
	 */
	private int executeLogin(CommandSourceStack source, String code) {
		LoginPlugin plugin = LoginPlugin.getInstance();

		LoginManager loginManager = plugin.getLoginManager();
		Logger logger = plugin.getLogger();
		FileConfiguration config = plugin.getConfig();

		// Load messages from config
		String loginSuccessMsg = config.getString("messages.login_success", "Successfully logged in!");
		String loginInvalidMsg = config.getString("messages.login_invalid", "Invalid login code!");

		// Create text components with appropriate colors
		TextComponent loginSuccess = Component.text(loginSuccessMsg, NamedTextColor.GREEN);
		TextComponent loginInvalid = Component.text(loginInvalidMsg, NamedTextColor.RED);

		Player player = (Player) source.getSender();
		if (!(player instanceof Player)) {
			source.getSender().sendMessage(Component.text("Only players can use this command.", NamedTextColor.RED));
			return 0;
		}

		logger.info("Login attempt by " + player.getName() + " with code: " + code);

		Boolean authorized = loginManager.authorizeWithCode(player.getName(), player.getUniqueId(), code);

		if (authorized) {
			player.sendMessage(loginSuccess);
			loginManager.setLoggedIn(player.getUniqueId());

			player.setInvulnerable(false);
			player.setWalkSpeed(0.2f);
			player.setFlySpeed(0.1f);
			player.setAllowFlight(false);
			player.setFlying(false);
		} else {
			player.sendMessage(loginInvalid);
			return 0;
		}

		return 1;
	}
}