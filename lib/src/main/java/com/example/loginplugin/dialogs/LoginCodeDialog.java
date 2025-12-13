package com.example.loginplugin.dialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.configuration.PluginMeta;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.event.ClickEvent;

/**
 * Registers login dialogs with text driven entirely by config.yml.
 */
public final class LoginCodeDialog {

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------
    private static final String INPUT_CODE_KEY = "code";
    private static final String ACTION_SUBMIT_KEY = "myplugin:submit_code";

    private static final String DIALOG_LOGIN = "myplugin:login_code";
    private static final String DIALOG_WRONG = "myplugin:login_code_wrong";

    // ----------------------------------------------------------------------
    private final LifecycleEventManager<BootstrapContext> manager;
    private final ComponentLogger logger;
    private final YamlConfiguration config;

    private final TextDialogInput codeInput;

    public LoginCodeDialog(BootstrapContext context) {
        this.manager = context.getLifecycleManager();
        this.logger = context.getLogger();

        logger.info(context.getDataDirectory().toString());

        // --- Data folder ---
        Path dataFolderPath = context.getDataDirectory();
        File dataFolder = dataFolderPath.toFile();
        if (!dataFolder.exists()) {
            boolean created = dataFolder.mkdirs();
            if (created) {
                logger.info("Created plugin data folder: " + dataFolder);
            }
        }

        // --- Config file ---
        File configFile = new File(dataFolder, "config.yml");

        // If file does not exist, create default
        if (!configFile.exists()) {
            try {
                boolean created = configFile.createNewFile();
                if (created) {
                    logger.info("Created default config.yml at " + configFile);
                    // Optionally, write default values
                    FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);
                    defaultConfig.set("dialogs.login.title", "Enter Discord Code");
                    defaultConfig.set("dialogs.login.info", "Enter the 6-digit code that the Discord bot sent you.");
                    defaultConfig.set("dialogs.login.input_label", "Code:");
                    defaultConfig.set("dialogs.login.buttons.submit", "Enter with code");
                    defaultConfig.set("dialogs.login.buttons.discord", "Go to Discord");

                    defaultConfig.set("dialogs.wrong_code.title", "Invalid Code");
                    defaultConfig.set("dialogs.wrong_code.info", "That code is incorrect. Please try again.");
                    defaultConfig.set("dialogs.wrong_code.input_label", "Code:");
                    defaultConfig.set("dialogs.wrong_code.buttons.submit", "Try Again");
                    defaultConfig.set("dialogs.wrong_code.buttons.discord", "Go to Discord");

                    defaultConfig.set("discord_url", "https://discord.gg/yourinvite");

                    defaultConfig.save(configFile);
                    logger.info("Default config.yml written.");
                }
            } catch (IOException e) {
                logger.error("Failed to create default config.yml", e);
            }
        }

        // Load configuration
        this.config = YamlConfiguration.loadConfiguration(configFile);
        logger.info("Loaded config.yml successfully.");

        this.codeInput = DialogInput.text(INPUT_CODE_KEY, Component.text("Code:")).build();

    }

    // ----------------------------------------------------------------------
    public void register() {
        manager.registerEventHandler(RegistryEvents.DIALOG.compose(),
                e -> e.registry()
                        .register(DialogKeys.create(Key.key("papermc:praise_paperchan")),
                                builder -> builder
                                        .base(DialogBase
                                                .builder(Component.text("Добро пожаловать!", NamedTextColor.YELLOW))
                                                .canCloseWithEscape(false)
                                                .body(List.of(
                                                        DialogBody.plainMessage(Component.text("By joining our server you agree that Paper-chan is cute!")),
                                                        DialogBody.plainMessage(Component.text("By joining our server you agree that Paper-chan is cute!")),
                                                        DialogBody.plainMessage(Component.text("By joining our server you agree that Paper-chan is cute!")),
                                                        DialogBody.plainMessage(Component.text("By joining our server you agree that Paper-chan is cute!")),
                                                        DialogBody.plainMessage(Component.text("By joining our server you agree that Paper-chan is cute!")),
                                                        DialogBody.plainMessage(Component.text("By joining our server you agree that Paper-chan is cute!"))
                                                ))
                                                .build())
                                        .type(DialogType.confirmation(
                                                ActionButton
                                                        .builder(Component.text("Paper-chan is cute!",
                                                                TextColor.color(0xEDC7FF)))
                                                        .tooltip(Component.text("Click to agree!"))
                                                        .action(DialogAction.customClick(
                                                                Key.key("papermc:paperchan/agree"), null))
                                                        .build(),
                                                ActionButton
                                                        .builder(Component.text("I hate Paper-chan!",
                                                                TextColor.color(0xFF8B8E)))
                                                        .tooltip(Component.text("Click this if you are a bad person!"))
                                                        .action(DialogAction.customClick(
                                                                Key.key("papermc:paperchan/disagree"), null))
                                                        .build()))));
    }

//		manager.registerEventHandler(RegistryEvents.DIALOG.compose(), event -> {
//
//			// Initial dialog
//			event.registry()
//					.register(DialogKeys.create(Key.key(DIALOG_LOGIN)),
//							builder -> builder.base(createBase(cfg("dialogs.login.title", "Enter Discord Code"),
//									NamedTextColor.YELLOW, cfg("dialogs.login.info", "Enter your 6-digit code."),
//									cfg("dialogs.login.input_label", "Code:")))
//									.type(createMultiAction(btnSubmit("dialogs.login.buttons.submit"),
//											btnDiscord("dialogs.login.buttons.discord"))));
//
//			// Wrong code dialog
//			event.registry()
//					.register(DialogKeys.create(Key.key(DIALOG_WRONG)),
//							builder -> builder.base(createBase(cfg("dialogs.wrong_code.title", "Invalid Code"),
//									NamedTextColor.RED, cfg("dialogs.wrong_code.info", "That code is incorrect."),
//									cfg("dialogs.wrong_code.input_label", "Code:")))
//									.type(createMultiAction(btnSubmit("dialogs.wrong_code.buttons.submit"),
//											btnDiscord("dialogs.wrong_code.buttons.discord"))));
//		});
    // ----------------------------------------------------------------------
    // Base dialog builder
    // ----------------------------------------------------------------------
    private DialogBase createBase(String title, NamedTextColor color, String message, String inputLabel) {
        TextDialogInput localInput = DialogInput.text(INPUT_CODE_KEY, Component.text(inputLabel)).build();

        return DialogBase.builder(Component.text(title, color)).canCloseWithEscape(false)
                .body(List.of(DialogBody.plainMessage(Component.text(message)))).inputs(List.of(localInput)).build();
    }

    // ----------------------------------------------------------------------
    // Multi-action layout
    // ----------------------------------------------------------------------
    private static DialogType createMultiAction(ActionButton left, ActionButton right) {
        return DialogType.multiAction(List.of(left, right), null, 2);
    }

    // ----------------------------------------------------------------------
    // Buttons
    // ----------------------------------------------------------------------
    private ActionButton btnSubmit(String configPath) {
        String label = cfg(configPath, "Submit");
        return ActionButton.builder(Component.text(label, NamedTextColor.GREEN))
                .action(DialogAction.customClick(Key.key(ACTION_SUBMIT_KEY), null)).build();
    }

    private ActionButton btnDiscord(String configPath) {
        String label = cfg(configPath, "Discord");
        String url = cfg("discord_url", "https://discord.gg/default");

        return ActionButton.builder(Component.text(label, NamedTextColor.BLUE))
                .action(DialogAction.staticAction(ClickEvent.openUrl(url))).build();
    }

    // ----------------------------------------------------------------------
    // Configuration helper
    // ----------------------------------------------------------------------
    /**
     * Reads string or returns fallback if missing.
     */
    private String cfg(String path, String fallback) {
        String v = config.getString(path);
        return (v == null || v.isBlank()) ? fallback : v;
    }
}
