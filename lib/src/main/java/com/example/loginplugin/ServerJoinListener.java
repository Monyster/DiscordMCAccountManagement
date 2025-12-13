package com.example.loginplugin;

import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.Map;
import java.util.UUID;

public class ServerJoinListener implements Listener {
	private final LoginManager loginManager;
	private final Logger logger;
	private final FileConfiguration config;

	private final Map<UUID, CompletableFuture<Boolean>> awaiting = new ConcurrentHashMap<>();

	public ServerJoinListener(LoginPlugin plugin) {
		this.loginManager = plugin.getLoginManager();
		this.logger = plugin.getLogger();
		this.config = plugin.getConfig();
	}

	@EventHandler
	void onPlayerConfigure(AsyncPlayerConnectionConfigureEvent event) {
		PlayerConfigurationConnection connection = event.getConnection();

		UUID uuid = connection.getProfile().getId();
		if (uuid == null)
			return;

		// show initial dialog and block until validation completes
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		future.completeOnTimeout(false, 60, TimeUnit.SECONDS);
		awaiting.put(uuid, future);

		DialogLike dialog = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG)
				.get(Key.key("myplugin:login_code"));
		connection.getAudience().showDialog(dialog);

		boolean ok = future.join(); // freeze until correct code
		connection.getAudience().closeDialog();
		awaiting.remove(uuid);

		if (!ok) {
			connection.disconnect(Component.text("Login failed or timed out.", NamedTextColor.RED));
		}
	}

	@EventHandler
	public void onDialogClick(PlayerCustomClickEvent event) {
		// only handle configuration connections
		if (!(event.getCommonConnection() instanceof PlayerConfigurationConnection connection))
			return;



		Key id = event.getIdentifier();
		
		logger.info(id.asString());

		// Only handle the submit button
		if (!id.equals(Key.key("myplugin:submit_code")))
			return;

		DialogResponseView view = event.getDialogResponseView();
		if (view == null) {
			return;
		}

		UUID playerUuid = connection.getProfile().getId();
		String playerName = connection.getProfile().getName();
		String code = view.getText("code");
		
		if (playerUuid == null || playerName == null || code == null)
			return;
		
		loginManager.remove(playerUuid);
		
		logger.info("Player " + playerName + " joined and must log in.");
		
		Boolean authorized = loginManager.authorizeWithCode(playerName, playerUuid, code);

		if (authorized) {
			// correct — complete and allow join
			complete(playerUuid, true);
			return;
		} else {
			// wrong — show retry dialog (do NOT complete the future)
			var retry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG)
					.get(Key.key("myplugin:login_code_wrong"));
			connection.getAudience().showDialog(retry);
			return;
		}
	}

	private void complete(UUID uuid, boolean val) {
		CompletableFuture<Boolean> f = awaiting.get(uuid);
		if (f != null && !f.isDone())
			f.complete(val);
	}

	@EventHandler
	public void onClose(PlayerConnectionCloseEvent e) {
		awaiting.remove(e.getPlayerUniqueId());
	}

	/**
	 * An event handler for cleanup the map to avoid unnecessary entry buildup.
	 */
	@EventHandler
	void onConnectionClose(PlayerConnectionCloseEvent event) {
		awaiting.remove(event.getPlayerUniqueId());
	}
}