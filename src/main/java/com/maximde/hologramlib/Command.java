package com.maximde.hologramlib;


import com.maximjsx.addonlib.core.AddonLib;
import com.maximjsx.addonlib.model.AddonEntry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


public class Command implements CommandExecutor {

    private final AddonLib addonLib;

    public Command(AddonLib addonLib) {
        this.addonLib = addonLib;
        this.addonLib.init();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, org.bukkit.command.@NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("hologramlib.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command!");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "upgrade":
                handleUpgrade(sender);
                break;
            case "list":
                handleList(sender);
                break;
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "Reloading HologramLib addons...");
        addonLib.reload(false);
        sender.sendMessage(ChatColor.GREEN + "Reload complete!");
    }

    private void handleUpgrade(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "Upgrading all addons...");
        addonLib.reload(true);
        sender.sendMessage(ChatColor.GREEN + "Upgrade complete!");
    }

    private void handleList(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "Installed addons:");
        for (Map.Entry<String, AddonEntry> entry :
                addonLib.getConfig().getAddonEntries().entrySet()) {
            String status = entry.getValue().isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled";
            sender.sendMessage(ChatColor.YELLOW + "- " + entry.getKey() + " v" +
                    entry.getValue().getInstalledVersion() + ": " + status);
            sender.sendMessage(ChatColor.GRAY + "  " + entry.getValue().getDescription());
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GREEN + "HologramLib Addon Commands:");
        sender.sendMessage(ChatColor.YELLOW + "/hologramlib reload - Reload addons");
        sender.sendMessage(ChatColor.YELLOW + "/hologramlib upgrade - Upgrade all addons");
        sender.sendMessage(ChatColor.YELLOW + "/hologramlib list - List installed addons");
    }
}