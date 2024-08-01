package com.tisawesomeness.endreset;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.generator.WorldInfo;
import org.bukkit.util.StringUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WhosInDangerCommand implements CommandExecutor, TabCompleter {

    private static final int MAIN_ISLAND_RADIUS = 256;

    private final EndReset plugin;
    public WhosInDangerCommand(EndReset plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            World theEnd = plugin.getServer().getWorld("world_the_end");
            if (theEnd == null) {
                plugin.sendMessage(sender, ChatColor.RED + "Could not check for players outside the main End island because world_the_end does not exist.");
            }
            tryRun(sender, theEnd, MAIN_ISLAND_RADIUS);
            return true;
        }
        if (args.length == 1) {
            plugin.sendMessage(sender, ChatColor.RED + "Usage: /" + label + " [<world> <radius>]");
            return true;
        }
        World world = plugin.getServer().getWorld(args[0]);
        if (world == null) {
            plugin.sendMessage(sender, ChatColor.RED + args[0] + " is not a valid world.");
            return true;
        }
        int radius = parseRadius(args[1]);
        if (radius == -1) {
            plugin.sendMessage(sender, ChatColor.RED + args[1] + " is not a valid radius.");
            return true;
        }
        tryRun(sender, world, radius);
        return true;
    }
    private int parseRadius(String str) {
        try {
            int radius = Integer.parseInt(str);
            if (radius > 0) {
                return radius;
            }
        } catch (NumberFormatException ignore) {
        }
        return -1;
    }

    private void tryRun(CommandSender sender, World world, int radius) {
        try {
            run(sender, world, radius);
        } catch (IOException e) {
            plugin.err(e);
            plugin.sendMessage(sender, ChatColor.RED + "An error occurred.");
        }
    }
    private void run(CommandSender sender, World world, int radius) throws IOException {
        List<OfflinePlayer> players = plugin.getPlayersOutside(world, radius);
        if (players.isEmpty()) {
            plugin.sendMessage(sender, "No players outside the reset area");
        } else {
            String playersStr = players.stream()
                    .map(OfflinePlayer::getName)
                    .collect(Collectors.joining(", "));
            plugin.sendMessage(sender, players.size() + " players outside the reset area: " + playersStr);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> worlds = plugin.getServer().getWorlds().stream()
                    .map(WorldInfo::getName)
                    .toList();
            return StringUtil.copyPartialMatches(args[0], worlds, new ArrayList<>());
        }
        return Collections.emptyList();
    }

}
