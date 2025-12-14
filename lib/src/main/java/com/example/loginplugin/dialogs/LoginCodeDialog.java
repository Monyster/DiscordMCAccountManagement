package com.example.loginplugin.dialogs;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.bukkit.configuration.file.YamlConfiguration;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
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
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

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
                    // Write default values
                    YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(configFile);

                    defaultConfig.set("server.name", "My Minecraft Server");
                    defaultConfig.set("server.description", "Welcome to our community server!");

                    defaultConfig.set("dialogs.login.title", "Welcome to the Server");
                    defaultConfig.set("dialogs.login.instruction", "Please go to our Discord server and get your 6-digit login code, then enter it below.");
                    defaultConfig.set("dialogs.login.input_label", "Enter Code:");
                    defaultConfig.set("dialogs.login.buttons.submit", "Submit Code");

                    defaultConfig.set("dialogs.wrong_code.title", "Invalid Code");
                    defaultConfig.set("dialogs.wrong_code.error_message", "❌ WRONG CODE - The code you entered is incorrect or expired.");
                    defaultConfig.set("dialogs.wrong_code.instruction", "Please check your Discord and enter the correct 6-digit code.");
                    defaultConfig.set("dialogs.wrong_code.input_label", "Enter Code:");
                    defaultConfig.set("dialogs.wrong_code.buttons.submit", "Try Again");

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
    }

    // ----------------------------------------------------------------------
    public void register() {
        manager.registerEventHandler(RegistryEvents.DIALOG.compose(), event -> {

            // Initial login dialog - submit button only
            event.registry()
                    .register(DialogKeys.create(Key.key(DIALOG_LOGIN)),
                            builder -> builder.base(createLoginDialogBase())
                                    .type(DialogType.multiAction(
                                            List.of(btnSubmit("dialogs.login.buttons.submit")),
                                            null,
                                            1)));

            // Wrong code dialog - submit button only
            event.registry()
                    .register(DialogKeys.create(Key.key(DIALOG_WRONG)),
                            builder -> builder.base(createWrongCodeDialogBase())
                                    .type(DialogType.multiAction(
                                            List.of(btnSubmit("dialogs.wrong_code.buttons.submit")),
                                            null,
                                            1)));
        });
    }
    // ----------------------------------------------------------------------
    // Dialog builders
    // ----------------------------------------------------------------------

    /**
     * Creates the initial login dialog with server name, description, and instructions.
     */
    private DialogBase createLoginDialogBase() {
        String serverName = cfg("server.name", "Minecraft Server");
        String serverDesc = cfg("server.description", "Welcome!");
        String title = cfg("dialogs.login.title", "Welcome to the Server");
        String instruction = cfg("dialogs.login.instruction", "Please go to our Discord server and get your 6-digit login code, then enter it below.");
        String inputLabel = cfg("dialogs.login.input_label", "Enter Code:");

        TextDialogInput codeInput = DialogInput.text(INPUT_CODE_KEY, Component.text(inputLabel)).build();

        return DialogBase.builder(Component.text(title, NamedTextColor.GOLD))
                .canCloseWithEscape(false)
                .body(List.of(
                        DialogBody.plainMessage(Component.text(serverName, NamedTextColor.YELLOW, TextDecoration.BOLD)),
                        DialogBody.plainMessage(Component.text(serverDesc, NamedTextColor.WHITE)),
                        DialogBody.plainMessage(Component.empty()),
                        DialogBody.plainMessage(Component.empty()),
                        DialogBody.plainMessage(Component.empty()),
                        DialogBody.plainMessage(Component.text(instruction, NamedTextColor.GRAY))
                ))
                .inputs(List.of(codeInput))
                .build();
    }

    /**
     * Creates the wrong code dialog with error message.
     */
    private DialogBase createWrongCodeDialogBase() {
        String title = cfg("dialogs.wrong_code.title", "Invalid Code");
        String errorMessage = cfg("dialogs.wrong_code.error_message", "❌ WRONG CODE - The code you entered is incorrect or expired.");
        String instruction = cfg("dialogs.wrong_code.instruction", "Please check your Discord and enter the correct 6-digit code.");
        String inputLabel = cfg("dialogs.wrong_code.input_label", "Enter Code:");

        TextDialogInput codeInput = DialogInput.text(INPUT_CODE_KEY, Component.text(inputLabel)).build();

        return DialogBase.builder(Component.text(title, NamedTextColor.RED))
                .canCloseWithEscape(false)
                .body(List.of(
                        DialogBody.plainMessage(Component.text(errorMessage, NamedTextColor.RED, TextDecoration.BOLD)),
                        DialogBody.plainMessage(Component.empty()),
                        DialogBody.plainMessage(Component.empty()),
                        DialogBody.plainMessage(Component.empty()),
                        DialogBody.plainMessage(Component.empty()),
                        DialogBody.plainMessage(Component.text(instruction, NamedTextColor.GRAY))
                ))
                .inputs(List.of(codeInput))
                .build();
    }

    // ----------------------------------------------------------------------
    // Buttons
    // ----------------------------------------------------------------------
    private ActionButton btnSubmit(String configPath) {
        String label = cfg(configPath, "Submit");
        return ActionButton.builder(Component.text(label, NamedTextColor.GREEN))
                .action(DialogAction.customClick(Key.key(ACTION_SUBMIT_KEY), null)).build();
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
