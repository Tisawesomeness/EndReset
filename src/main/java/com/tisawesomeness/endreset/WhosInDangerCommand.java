package com.tisawesomeness.endreset;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class WhosInDangerCommand implements CommandExecutor {

    private final EndReset plugin;
    public WhosInDangerCommand(EndReset plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            run(sender);
        } catch (IOException e) {
            e.printStackTrace();
            plugin.sendMessage(sender, ChatColor.RED + "An error occurred.");
        }
        return true;
    }
    private void run(CommandSender sender) throws IOException {
        List<OfflinePlayer> players = plugin.getPlayersOutsideEndIsland();
        if (players.isEmpty()) {
            plugin.sendMessage(sender, "No players outside the main end island");
        } else {
            String playersStr = players.stream().map(OfflinePlayer::getName).collect(Collectors.joining(", "));
            plugin.sendMessage(sender, players.size() + " players outside the main end island: " + playersStr);
        }
    }

}
