package com.tisawesomeness.endreset;

import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.collection.ListTag;
import dev.dewy.nbt.tags.primitive.DoubleTag;
import dev.dewy.nbt.tags.primitive.StringTag;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class EndReset extends JavaPlugin {

    private static final int MAIN_ISLAND_RADIUS = 256;

    private static final Nbt NBT = new Nbt();

    @Override
    public void onEnable() {
        try {
            logPlayersOutsideEndIsland();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        Objects.requireNonNull(getCommand("whosindanger")).setExecutor(new WhosInDangerCommand(this));
    }

    private void logPlayersOutsideEndIsland() throws IOException {
        List<OfflinePlayer> players = getPlayersOutsideEndIsland();
        if (players.isEmpty()) {
            getLogger().info("No players outside the main End island");
        } else {
            String playersStr = players.stream()
                    .map(EndReset::toLogStr)
                    .collect(Collectors.joining("\n  "));
            getLogger().info(players.size() + " players outside the main End island:\n  " + playersStr);
        }
    }
    private static String toLogStr(OfflinePlayer player) {
        return player.getUniqueId() + " " + player.getName();
    }

    public List<OfflinePlayer> getPlayersOutsideEndIsland() throws IOException {

        File[] playerData = getPlayerDataFolder().listFiles(f -> f.getName().endsWith(".dat"));
        if (playerData == null) {
            throw new IOException("An error occurred while reading the playerdata folder.");
        }

        List<OfflinePlayer> playersToReset = new ArrayList<>();
        for (File playerDataFile : playerData) {
            OfflinePlayer player = parsePlayerFromFileName(playerDataFile);
            if (player == null) {
                getLogger().warning("Invalid UUID in playerdata folder: " + playerDataFile.getName());
                continue;
            }
            Player onlinePlayer = player.getPlayer();
            if (onlinePlayer == null ? shouldBeReset(playerDataFile) : shouldBeReset(onlinePlayer)) {
                playersToReset.add(player);
            }
        }
        return playersToReset;

    }

    private File getPlayerDataFolder() throws FileNotFoundException {
        for (World world : getServer().getWorlds()) {
            File maybePlayerdata = world.getWorldFolder().toPath().resolve("playerdata").toFile();
            if (maybePlayerdata.isDirectory()) {
                return maybePlayerdata;
            }
        }
        throw new FileNotFoundException("Could not find player data folder");
    }

    // Assumes the file name ends in ".dat"
    private OfflinePlayer parsePlayerFromFileName(File playerDataFile) {
        String fileName = playerDataFile.getName();
        String uuidStr = fileName.substring(0, fileName.length() - 4);
        try {
            return getServer().getOfflinePlayer(UUID.fromString(uuidStr));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    private boolean shouldBeReset(File playerDataFile) throws IOException {
        CompoundTag nbt = NBT.fromFile(playerDataFile);

        StringTag dimension = nbt.getString("Dimension");
        ListTag<DoubleTag> pos = nbt.getList("Pos");
        return dimension.getValue().equals("minecraft:the_end") && !isInsideMainIsland(pos);
    }
    private boolean isInsideMainIsland(ListTag<DoubleTag> pos) {
        double x = pos.get(0).getValue();
        double z = pos.get(2).getValue();
        return Math.max(Math.abs(x), Math.abs(z)) <= MAIN_ISLAND_RADIUS;
    }

    private boolean shouldBeReset(Player player) {
        if (player.getWorld().getEnvironment() != World.Environment.THE_END) {
            return false;
        }
        Location loc = player.getLocation();
        return Math.max(Math.abs(loc.getX()), Math.abs(loc.getZ())) > MAIN_ISLAND_RADIUS;
    }

    public void sendMessage(CommandSender sender, String msg, Object... args) {
        sender.sendMessage("§7[§5ER§7]§r " + String.format(msg, args));
    }

}
