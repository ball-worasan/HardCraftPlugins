package com.hardcraft.core.command;

import com.hardcraft.core.HardCraftCore;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

public final class HardCraftCommand implements CommandExecutor, TabCompleter {
    private final HardCraftCore plugin;

    public HardCraftCommand(HardCraftCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String[] args) {
        String action = args.length == 0 ? "status" : args[0].toLowerCase(Locale.ROOT);
        if (action.equals("status")) {
            if (!sender.hasPermission("hardcraft.core.status")) return noPermission(sender);
            sender.sendMessage(Component.text("HardCraftCore " + plugin.getPluginMeta().getVersion()
                    + " | API " + plugin.contractVersion()
                    + " | database " + (plugin.settings().databaseEnabled() ? "enabled" : "not configured")
                    + " | migration " + plugin.migrationState()));
            return true;
        }
        if (action.equals("reload")) {
            if (!sender.hasPermission("hardcraft.core.reload")) return noPermission(sender);
            try {
                plugin.reloadValidatedConfig();
                sender.sendMessage(Component.text("HardCraftCore configuration reloaded."));
            } catch (RuntimeException exception) {
                sender.sendMessage(Component.text("Reload rejected: " + exception.getMessage()));
            }
            return true;
        }
        sender.sendMessage(Component.text("Usage: /" + label + " [status|reload]"));
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
            @NotNull String alias, @NotNull String[] args) {
        if (args.length != 1) return List.of();
        String input = args[0].toLowerCase(Locale.ROOT);
        return List.of("status", "reload").stream()
                .filter(value -> !value.equals("reload") || sender.hasPermission("hardcraft.core.reload"))
                .filter(value -> value.startsWith(input)).toList();
    }

    private boolean noPermission(CommandSender sender) {
        sender.sendMessage(Component.text("You do not have permission."));
        return true;
    }
}