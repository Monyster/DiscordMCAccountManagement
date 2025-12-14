package com.example.loginplugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.dialog.DialogLike;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ServerJoinListener implements Listener {

    private final LoginPlugin plugin;
    private final LoginManager loginManager;
    private final Logger logger;

    private final Map<UUID, CompletableFuture<Boolean>> awaiting = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> attemptCounts = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> successfulLogins = new ConcurrentHashMap<>();

    // Maximum failed attempts before disconnect
    private static final int MAX_ATTEMPTS = 3;

    // Mocked skin texture data
    private static final String SKIN_TEXTURE_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTYyMzg1ODYzMDY2NCwKICAicHJvZmlsZUlkIiA6ICIyYzEwNjRmY2Q5MTc0MjgyODRlM2JmN2ZhYTdlM2UxYSIsCiAgInByb2ZpbGVOYW1lIiA6ICJOYWVtZSIsCiAgInNpZ25hdHVyZVJlcXVpcmVkIiA6IHRydWUsCiAgInRleHR1cmVzIiA6IHsKICAgICJTS0lOIiA6IHsKICAgICAgInVybCIgOiAiaHR0cDovL3RleHR1cmVzLm1pbmVjcmFmdC5uZXQvdGV4dHVyZS8zOTI3ZTZkZWZjZjNlZTgwMzRkYjBmMTI2YTRjN2M0ZGQ2ODhlYTkwZmVkMzkzMWUyZjE5NDM3M2YzYjRmMjdhIgogICAgfQogIH0KfQ==";
    private static final String SKIN_TEXTURE_SIGNATURE = "HEqglaa6nzRkcipQYG8k/HZq+5fhJcE2kGZM9uuG3Tr+PsUZvMco5KaDjItgUPw/Meq9oeQ53oAqEsUiZ2piQ7gnX6jbrPgwsHuW13ZWS4dw0z0XaQo2S8X2k079jU8D5x+oPVOFkpKuijXzA9p9keHjSMR7oK+yK+BuEnc45FsQw+D0Zp+cWsYfxGuytYHQWqc0B0Q/MDpqKL+DEUJ4nsI/VVtJm0jcyf/l5TZil6YuO4bOQo6lSOOD73mMb+pj5Xq/acRRbYsSqfeRad69fByE37MUZQyAH/JUiGDHoK5iSwGOa5NwZRUwx61T10X7us6PxGwq30jFivdJ62YUtgHU/twwbUu6S2+GyPdPxeFf+leU9iNfEVghARDkKHZbIaURMqkPsfy6aGvxodyu80goqE8cbYCCZqaQYB+89dl5/ANP7sNoSVcCWHO30Gd823IHVqACf97IpUhV7ob+vHIu6P3q3FXShnKApRC2wMyFB6W8xBqdex0z7lqWO48JaokXqPEyC66cnhkjJEESokahLzIIk4JFzMwFJFZM6h8i5I9lja7q5LzsiRc9k1ZG4l+PqJjTTgOXabkZXp1xfrrXZY2MheZUyIjkoNP6BeqHqbPZ4an1l7WpRinsvl/ggF7FA0EAc8XLm6z/31kIdsVpSbfWjxeL0RT8UqOrGQw=";

    public ServerJoinListener(LoginPlugin plugin) {
        this.plugin = plugin;
        this.loginManager = plugin.getLoginManager();
        this.logger = plugin.getLogger();
    }

    @EventHandler
    void onPlayerConfigure(AsyncPlayerConnectionConfigureEvent event) {
        PlayerConfigurationConnection connection = event.getConnection();

        UUID uuid = connection.getProfile().getId();
        String playerName = connection.getProfile().getName();

        if (uuid == null) {
            return;
        }

        logger.info("========================================");
        logger.info("Player " + playerName + " entered configuration phase");
        logger.info("This is when 'Connecting to world...' screen is shown");
        logger.info("========================================");

        // Initialize attempt counter for this player
        attemptCounts.put(uuid, 0);

        // show initial dialog and block until validation completes
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        future.completeOnTimeout(false, 60, TimeUnit.SECONDS);
        awaiting.put(uuid, future);

        logger.info("Player " + playerName + " - Showing login dialog now...");

        DialogLike dialog = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG)
                .get(Key.key("myplugin:login_code"));
        connection.getAudience().showDialog(dialog);

        logger.info("Player " + playerName + " - Waiting for code submission (FROZEN on 'Connecting to world...')");

        boolean ok = future.join(); // freeze until correct code

        logger.info("Player " + playerName + " - Code validation completed. Result: " + (ok ? "SUCCESS" : "FAILED"));

        connection.getAudience().closeDialog();
        awaiting.remove(uuid);
        attemptCounts.remove(uuid);

        if (!ok) {
            logger.info("Player " + playerName + " - Disconnecting due to failed/timed out login");
            connection.disconnect(Component.text("Login failed or timed out.", NamedTextColor.RED));
        } else {
            logger.info("Player " + playerName + " - Configuration phase complete, allowing join to server");
            logger.info("========================================");
            // Mark player as successfully logged in so we can set their skin on join
            successfulLogins.put(uuid, true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // Check if player successfully logged in
        if (successfulLogins.remove(uuid)) {
            logger.info("Player " + player.getName() + " joined - applying skin texture...");

            try {
                // Get player profile
                PlayerProfile profile = player.getPlayerProfile();

                // Remove existing textures
                profile.removeProperty("textures");

                // Add new skin texture with value and signature
                profile.setProperty(new ProfileProperty("textures", SKIN_TEXTURE_VALUE, SKIN_TEXTURE_SIGNATURE));

                // Apply the profile to the player
                player.setPlayerProfile(profile);

                logger.info("Player " + player.getName() + " - Skin texture applied successfully!");

                // Refresh the player's appearance for other players
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (!onlinePlayer.equals(player)) {
                            onlinePlayer.hidePlayer(plugin, player);
                            onlinePlayer.showPlayer(plugin, player);
                        }
                    }
                    logger.info("Player " + player.getName() + " - Skin refreshed for other players");
                }, 20L); // Wait 1 second (20 ticks) before refreshing

            } catch (Exception e) {
                logger.severe("Failed to apply skin texture for player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onDialogClick(PlayerCustomClickEvent event) {
        // only handle configuration connections
        if (!(event.getCommonConnection() instanceof PlayerConfigurationConnection connection)) {
            return;
        }

        Key id = event.getIdentifier();

        logger.info(id.asString());

        // Handle submit button
        if (!id.equals(Key.key("myplugin:submit_code"))) {
            return;
        }

        DialogResponseView view = event.getDialogResponseView();
        if (view == null) {
            return;
        }

        UUID playerUuid = connection.getProfile().getId();
        String playerName = connection.getProfile().getName();
        String code = view.getText("code");

        if (playerUuid == null || playerName == null || code == null) {
            return;
        }

        logger.info("Player " + playerName + " attempting login.");

        Boolean authorized = loginManager.authorizeWithCode(playerName, playerUuid, code);

        if (authorized) {
            // correct — complete and allow join
            complete(playerUuid, true);
            return;
        } else {
            // wrong code — increment attempt counter
            int attempts = attemptCounts.getOrDefault(playerUuid, 0) + 1;
            attemptCounts.put(playerUuid, attempts);

            logger.warning("Player " + playerName + " failed login attempt " + attempts + "/" + MAX_ATTEMPTS);

            // Check if max attempts exceeded
            if (attempts >= MAX_ATTEMPTS) {
                logger.warning("Player " + playerName + " exceeded max login attempts. Disconnecting.");
                connection.getAudience().closeDialog();
                complete(playerUuid, false);
                connection.disconnect(Component.text("Too many failed login attempts.", NamedTextColor.RED));
                return;
            }

            // Show retry dialog (do NOT complete the future)
            var retry = RegistryAccess.registryAccess().getRegistry(RegistryKey.DIALOG)
                    .get(Key.key("myplugin:login_code_wrong"));
            connection.getAudience().showDialog(retry);
            return;
        }
    }

    private void complete(UUID uuid, boolean val) {
        CompletableFuture<Boolean> f = awaiting.get(uuid);
        if (f != null && !f.isDone()) {
            f.complete(val);
        }
    }

    @EventHandler
    public void onConnectionClose(PlayerConnectionCloseEvent event) {
        UUID uuid = event.getPlayerUniqueId();
        awaiting.remove(uuid);
        attemptCounts.remove(uuid);
        successfulLogins.remove(uuid);
    }
}
