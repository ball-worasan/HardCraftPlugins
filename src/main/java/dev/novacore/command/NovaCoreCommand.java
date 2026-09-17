package dev.novacore.command;

import dev.novacore.NovaCore;
import dev.novacore.manager.ConfigManager;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NovaCoreCommand implements CommandExecutor, TabCompleter {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final List<String> SUBCOMMANDS = List.of("help", "info", "reload");
    private final NovaCore plugin;
    private final ConfigManager configManager;

    public NovaCoreCommand(NovaCore plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return true;
        }
        if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(Component.text(plugin.getName() + " v" + plugin.getPluginMeta().getVersion()));
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("novacore.admin")) {
                sendConfiguredMessage(sender, "no-permission");
                return true;
            }
            configManager.reload();
            sendConfiguredMessage(sender, "reload-success");
            return true;
        }

        sendConfiguredMessage(sender, "unknown-command");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String input = args[0].toLowerCase(Locale.ROOT);
        return SUBCOMMANDS.stream()
                .filter(subcommand -> !subcommand.equals("reload") || sender.hasPermission("novacore.admin"))
                .filter(subcommand -> subcommand.startsWith(input))
                .toList();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(Component.text("NovaCore commands:"));
        sender.sendMessage(Component.text("/" + label + " help - Show available commands"));
        sender.sendMessage(Component.text("/" + label + " info - Show plugin information"));
        if (sender.hasPermission("novacore.admin")) {
            sender.sendMessage(Component.text("/" + label + " reload - Reload configuration"));
        }
    }

    private void sendConfiguredMessage(CommandSender sender, String path) {
        sender.sendMessage(MINI_MESSAGE.deserialize(
                configManager.getMessage("prefix") + configManager.getMessage(path)));
    }
}