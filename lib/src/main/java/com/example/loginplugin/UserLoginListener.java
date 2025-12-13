package com.example.loginplugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class UserLoginListener implements Listener {

	private final LoginManager loginManager;
	private final Logger logger;
	private final FileConfiguration config;

	public UserLoginListener(LoginPlugin plugin) {
		this.loginManager = plugin.getLoginManager();
		this.logger = plugin.getLogger();
		this.config = plugin.getConfig();
	}

	/**
	 * Returns true if player is logged in. Does NOT send message (to avoid spam).
	 */
	private boolean isLogged(Player player) {
		return loginManager.isLoggedIn(player.getUniqueId());
	}

	/**
	 * Sends login message **once per blocked action**.
	 */
	private void notify(Player player) {
		player.sendMessage(Component.text("You must login first: /login <code>", NamedTextColor.RED));
	}

	// ---------------------------
	// Movement block
	// ---------------------------
	@EventHandler(ignoreCancelled = true)
	public void onMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		if (!isLogged(p)) {
			e.setTo(e.getFrom());
		}
	}

	// ---------------------------
	// Chat block
	// ---------------------------
	@EventHandler(ignoreCancelled = true)
	public void onChat(AsyncChatEvent e) {
		Player p = e.getPlayer();
		if (!isLogged(p)) {
			e.setCancelled(true);
			notify(p);
		}
	}

	// ---------------------------
	// Command block (allow only /login)
	// ---------------------------
	@EventHandler(ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent e) {
		Player p = e.getPlayer();
		if (isLogged(p))
			return;

		String msg = e.getMessage().toLowerCase();

		if (msg.startsWith("/login")) {
			logger.fine("Allowing /login for " + p.getName());
			return; // allow
		}

		logger.fine("Blocking command " + msg + " for " + p.getName());
		e.setCancelled(true);
		notify(p);
	}

	// ---------------------------
	// Block break / place
	// ---------------------------
	@EventHandler(ignoreCancelled = true)
	public void onBreak(BlockBreakEvent e) {
		Player p = e.getPlayer();
		if (!isLogged(p))
			e.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPlace(BlockPlaceEvent e) {
		Player p = e.getPlayer();
		if (!isLogged(p))
			e.setCancelled(true);
	}

	// ---------------------------
	// Damage block
	// ---------------------------
	@EventHandler(ignoreCancelled = true)
	public void onDamage(EntityDamageEvent e) {
		if (e.getEntity() instanceof Player p && !isLogged(p)) {
			e.setCancelled(true);
		}
	}

	// ---------------------------
	// Inventory open / click block
	// ---------------------------
	@EventHandler(ignoreCancelled = true)
	public void onInvOpen(InventoryOpenEvent e) {
		if (e.getPlayer() instanceof Player p && !isLogged(p)) {
			e.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	public void onInvClick(InventoryClickEvent e) {
		if (e.getWhoClicked() instanceof Player p && !isLogged(p)) {
			e.setCancelled(true);
		}
	}

	// ---------------------------
	// Item drop / pickup block
	// ---------------------------
	@EventHandler(ignoreCancelled = true)
	public void onDrop(PlayerDropItemEvent e) {
		if (!isLogged(e.getPlayer()))
			e.setCancelled(true);
	}

	@EventHandler(ignoreCancelled = true)
	public void onPickup(EntityPickupItemEvent e) {
		if (e.getEntity() instanceof Player p && !isLogged(p)) {
			e.setCancelled(true);
		}
	}

	// ---------------------------
	// Swap hand block (F key)
	// ---------------------------
	@EventHandler(ignoreCancelled = true)
	public void onSwap(PlayerSwapHandItemsEvent e) {
		if (!isLogged(e.getPlayer()))
			e.setCancelled(true);
	}

	// ---------------------------
	// Player Join
	// ---------------------------
	@EventHandler(ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		// If not registered → disconnect immediately
		if (!loginManager.isRegistered(player.getName())) {
			player.kick(Component.text("You are not registered!\nPlease visit:\nhttps://example.com/register",
					NamedTextColor.RED));
			return;
		}

		// Freeze without anti-cheat issues
		player.setInvulnerable(true);
		player.setAllowFlight(true);
		player.setFlying(true);
		player.setWalkSpeed(0f);
		player.setFlySpeed(0f);
		player.teleport(player.getLocation());

		// Force logout
		loginManager.remove(player.getUniqueId());

		logger.info("Player " + player.getName() + " joined and must log in.");

		String msg = config.getString("messages.login_prompt", "Please use /login <code>");
		player.sendMessage(msg);
	}
}
