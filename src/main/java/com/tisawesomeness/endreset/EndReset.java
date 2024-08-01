package com.tisawesomeness.endreset;

import dev.dewy.nbt.Nbt;
import dev.dewy.nbt.tags.collection.CompoundTag;
import dev.dewy.nbt.tags.collection.ListTag;
import dev.dewy.nbt.tags.primitive.DoubleTag;
import dev.dewy.nbt.tags.primitive.StringTag;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class EndReset extends JavaPlugin {

    private static final Nbt NBT = new Nbt();

    @Override
    public void onEnable() {
        Objects.requireNonNull(getCommand("whosindanger")).setExecutor(new WhosInDangerCommand(this));
    }

    public List<OfflinePlayer> getPlayersOutside(World world, int radius) throws IOException {

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
            if (onlinePlayer == null ? shouldBeReset(playerDataFile, world, radius) : shouldBeReset(onlinePlayer, world, radius)) {
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
    private boolean shouldBeReset(File playerDataFile, World world, int radius) throws IOException {
        CompoundTag nbt = NBT.fromFile(playerDataFile);

        StringTag dimension = nbt.getString("Dimension");
        ListTag<DoubleTag> pos = nbt.getList("Pos");

        NamespacedKey dimensionKey = NamespacedKey.fromString(dimension.getValue());
        return dimensionKey != null && dimensionKey.getKey().equals(world.getKey().getKey()) && !isInsideRadius(pos, radius);
    }
    private boolean isInsideRadius(ListTag<DoubleTag> pos, int radius) {
        double x = pos.get(0).getValue();
        double z = pos.get(2).getValue();
        return Math.max(Math.abs(x), Math.abs(z)) <= radius;
    }

    private boolean shouldBeReset(Player player, World world, int radius) {
        if (!player.getWorld().equals(world)) {
            return false;
        }
        Location loc = player.getLocation();
        return Math.max(Math.abs(loc.getX()), Math.abs(loc.getZ())) > radius;
    }

    public void sendMessage(CommandSender sender, String msg, Object... args) {
        sender.sendMessage("§7[§5ER§7]§r " + String.format(msg, args));
    }

    public void err(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        for (String line : sw.toString().split("\n")) {
            getLogger().severe(line);
        }
    }

}
