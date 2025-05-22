package com.maximde.hologramlib.persistence;

import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.maximde.hologramlib.HologramLib;
import com.maximde.hologramlib.hologram.*;
import com.maximde.hologramlib.utils.Vector3F;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class PersistenceManager {

    private final File configFile;
    private FileConfiguration config;
    @Getter
    private final Set<String> persistentHolograms = new HashSet<>();
    @Getter
    private final Set<String> persistentLeaderboards = new HashSet<>();

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public PersistenceManager() {
        configFile = new File(HologramLib.getPlugin().getDataFolder(), "holograms.yml");
        if (!configFile.exists()) {
            try {
                HologramLib.getPlugin().getDataFolder().mkdirs();
                configFile.createNewFile();
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.SEVERE, "Failed to create holograms.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void saveHologram(Hologram<?> hologram) {
        persistentHolograms.add(hologram.getId());
        config.set("holograms." + hologram.getId() + ".type", getHologramType(hologram));
        config.set("holograms." + hologram.getId() + ".renderMode", hologram.getRenderMode().name());

        Location loc = hologram.getLocation();
        config.set("holograms." + hologram.getId() + ".location.world", Objects.requireNonNull(loc.getWorld()).getName());
        config.set("holograms." + hologram.getId() + ".location.x", loc.getX());
        config.set("holograms." + hologram.getId() + ".location.y", loc.getY());
        config.set("holograms." + hologram.getId() + ".location.z", loc.getZ());

        saveCommonProperties(hologram, "holograms." + hologram.getId());

        if (hologram instanceof TextHologram textHologram) {
            saveTextHologram(textHologram, "holograms." + hologram.getId());
        } else if (hologram instanceof ItemHologram itemHologram) {
            saveItemHologram(itemHologram, "holograms." + hologram.getId());
        } else if (hologram instanceof BlockHologram blockHologram) {
            saveBlockHologram(blockHologram, "holograms." + hologram.getId());
        }

        saveConfig();
    }

    public void saveLeaderboard(LeaderboardHologram leaderboard) {
        String id = leaderboard.getTextHologram().getId();
        persistentLeaderboards.add(id);

        saveHologram(leaderboard.getTextHologram());
        saveHologram(leaderboard.getFirstPlaceHead());

        config.set("leaderboards." + id + ".textHologramId", leaderboard.getTextHologram().getId());
        config.set("leaderboards." + id + ".headHologramId", leaderboard.getFirstPlaceHead().getId());

        LeaderboardHologram.LeaderboardOptions options = leaderboard.getOptions();
        config.set("leaderboards." + id + ".options.title", options.title());
        config.set("leaderboards." + id + ".options.scale", options.scale());
        config.set("leaderboards." + id + ".options.topPlayerHead", options.topPlayerHead());
        config.set("leaderboards." + id + ".options.showEmptyPlaces", options.showEmptyPlaces());
        config.set("leaderboards." + id + ".options.maxDisplayEntries", options.maxDisplayEntries());
        config.set("leaderboards." + id + ".options.suffix", options.suffix());

        for (int i = 0; i < options.placeFormats().length; i++) {
            config.set("leaderboards." + id + ".options.placeFormats." + i, options.placeFormats()[i]);
        }

        config.set("leaderboards." + id + ".options.defaultPlaceFormat", options.defaultPlaceFormat());
        config.set("leaderboards." + id + ".options.titleFormat", options.titleFormat());
        config.set("leaderboards." + id + ".options.footerFormat", options.footerFormat());

        saveConfig();
    }

    public void removeHologram(String id) {
        persistentHolograms.remove(id);
        config.set("holograms." + id, null);
        saveConfig();
    }

    public void removeLeaderboard(String id) {
        persistentLeaderboards.remove(id);

        String textHologramId = config.getString("leaderboards." + id + ".textHologramId");
        String headHologramId = config.getString("leaderboards." + id + ".headHologramId");

        if (textHologramId != null) {
            removeHologram(textHologramId);
        }

        if (headHologramId != null) {
            removeHologram(headHologramId);
        }

        config.set("leaderboards." + id, null);
        saveConfig();
    }

    private void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to save holograms.yml", e);
        }
    }

    private String getHologramType(Hologram<?> hologram) {
        if (hologram instanceof TextHologram) return "TEXT";
        if (hologram instanceof ItemHologram) return "ITEM";
        if (hologram instanceof BlockHologram) return "BLOCK";
        return "UNKNOWN";
    }

    private void saveCommonProperties(Hologram<?> hologram, String path) {
        Vector3F scale = hologram.getScale();
        config.set(path + ".scale.x", scale.x);
        config.set(path + ".scale.y", scale.y);
        config.set(path + ".scale.z", scale.z);

        Vector3F translation = hologram.getTranslation();
        config.set(path + ".translation.x", translation.x);
        config.set(path + ".translation.y", translation.y);
        config.set(path + ".translation.z", translation.z);

        config.set(path + ".billboard", hologram.getBillboard().name());

        config.set(path + ".teleportDuration", hologram.getTeleportDuration());
        config.set(path + ".interpolationDurationTransformation", hologram.getInterpolationDurationTransformation());
        config.set(path + ".viewRange", hologram.getViewRange());
        config.set(path + ".updateTaskPeriod", hologram.getUpdateTaskPeriod());
        config.set(path + ".maxPlayerRenderDistanceSquared", hologram.getMaxPlayerRenderDistanceSquared());
    }

    private void saveTextHologram(TextHologram hologram, String path) {
        MiniMessage miniMessage = MiniMessage.miniMessage();
        String serializedText = miniMessage.serialize(hologram.getTextAsComponent());
        config.set(path + ".text", serializedText);
        config.set(path + ".shadow", hologram.isShadow());
        config.set(path + ".maxLineWidth", hologram.getMaxLineWidth());
        config.set(path + ".backgroundColor", hologram.getBackgroundColor());
        config.set(path + ".seeThroughBlocks", hologram.isSeeThroughBlocks());
        config.set(path + ".alignment", hologram.getAlignment().name());
        config.set(path + ".textOpacity", hologram.getTextOpacity());
    }

    private void saveItemHologram(ItemHologram hologram, String path) {
        config.set(path + ".displayType", hologram.getDisplayType().name());
        config.set(path + ".onFire", hologram.isOnFire());
        config.set(path + ".glowing", hologram.isGlowing());
        config.set(path + ".glowColor", hologram.getGlowColor());
        config.set(path + ".item.type", hologram.getItem().getType().getName().getKey());
    }

    private void saveBlockHologram(BlockHologram hologram, String path) {
        config.set(path + ".block", hologram.getBlock());
        config.set(path + ".onFire", hologram.isOnFire());
        config.set(path + ".glowing", hologram.isGlowing());
        config.set(path + ".glowColor", hologram.getGlowColor());
    }

    public void loadHolograms() {
        if (config.contains("holograms")) {
            ConfigurationSection hologramsSection = config.getConfigurationSection("holograms");
            if (hologramsSection != null) {
                for (String id : hologramsSection.getKeys(false)) {
                    try {
                        String path = "holograms." + id;
                        String type = config.getString(path + ".type");
                        RenderMode renderMode = RenderMode.valueOf(config.getString(path + ".renderMode"));

                        String worldName = config.getString(path + ".location.world");
                        double x = config.getDouble(path + ".location.x");
                        double y = config.getDouble(path + ".location.y");
                        double z = config.getDouble(path + ".location.z");
                        assert worldName != null;
                        Location location = new Location(Bukkit.getWorld(worldName), x, y, z);
                        if ("TEXT".equals(type)) {
                            loadTextHologram(id, renderMode, location, path);
                        } else if ("ITEM".equals(type)) {
                            loadItemHologram(id, renderMode, location, path);
                        } else if ("BLOCK".equals(type)) {
                            loadBlockHologram(id, renderMode, location, path);
                        }

                        persistentHolograms.add(id);
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "Failed to load hologram: " + id, e);
                    }
                }
            }
        }

        if (config.contains("leaderboards")) {
            ConfigurationSection leaderboardsSection = config.getConfigurationSection("leaderboards");
            if (leaderboardsSection != null) {
                for (String id : leaderboardsSection.getKeys(false)) {
                    try {
                        loadLeaderboard(id);
                        persistentLeaderboards.add(id);
                    } catch (Exception e) {
                        Bukkit.getLogger().log(Level.SEVERE, "Failed to load leaderboard: " + id, e);
                    }
                }
            }
        }
    }

    private void loadTextHologram(String id, RenderMode renderMode, Location location, String path) {
        TextHologram hologram = new TextHologram(id, renderMode);
        String serializedText = config.getString(path + ".text");
        if(serializedText != null) {
            MiniMessage miniMessage = MiniMessage.miniMessage();
            Component text = miniMessage.deserialize(serializedText);
            hologram.setText(text);
        }
        hologram.setShadow(config.getBoolean(path + ".shadow", true));
        hologram.setMaxLineWidth(config.getInt(path + ".maxLineWidth", 200));
        hologram.setBackgroundColor(config.getInt(path + ".backgroundColor", 0));
        hologram.setSeeThroughBlocks(config.getBoolean(path + ".seeThroughBlocks", false));

        String alignmentStr = config.getString(path + ".alignment", "CENTER");
        hologram.setAlignment(TextDisplay.TextAlignment.valueOf(alignmentStr));

        hologram.setTextOpacity(config.contains(path + ".textOpacity") ?
                (byte) config.getInt(path + ".textOpacity") : (byte) -1);

        applyCommonProperties(hologram, path);
        HologramLib.getManager().ifPresent(manager -> manager.spawn(hologram, location, true));
    }

    private void loadItemHologram(String id, RenderMode renderMode, Location location, String path) {
        ItemHologram hologram = new ItemHologram(id, renderMode);

        Color color = new Color(config.getInt(path + ".glowColor", Color.YELLOW.getRGB()), true);


        String displayTypeStr = config.getString(path + ".displayType", "FIXED");
        hologram.setDisplayType(me.tofaa.entitylib.meta.display.ItemDisplayMeta.DisplayType.valueOf(displayTypeStr));

        hologram.setOnFire(config.getBoolean(path + ".onFire", false));
        hologram.setGlowing(config.getBoolean(path + ".glowing", false));
        hologram.setGlowColor(color);
        hologram.setItem(ItemStack.builder().type(Objects.requireNonNull(ItemTypes.getByName(config.getString(path + ".item.type", "air").toLowerCase()))).build());

        applyCommonProperties(hologram, path);
        HologramLib.getManager().ifPresent(manager -> manager.spawn(hologram, location, true));
    }

    private void loadBlockHologram(String id, RenderMode renderMode, Location location, String path) {
        BlockHologram hologram = new BlockHologram(id, renderMode);

        Color color = new Color(config.getInt(path + ".glowColor", Color.YELLOW.getRGB()), true);

        hologram.setBlock(config.getInt(path + ".block", 0));
        hologram.setOnFire(config.getBoolean(path + ".onFire", false));
        hologram.setGlowing(config.getBoolean(path + ".glowing", false));
        hologram.setGlowColor(color);

        applyCommonProperties(hologram, path);
        HologramLib.getManager().ifPresent(manager -> manager.spawn(hologram, location, true));
    }

    private void loadLeaderboard(String id) {
        String path = "leaderboards." + id;

        String textHologramId = config.getString(path + ".textHologramId");
        String headHologramId = config.getString(path + ".headHologramId");

        Optional<Hologram<?>> textHologramOpt = HologramLib.getManager().flatMap(m -> m.getHologram(textHologramId));
        Optional<Hologram<?>> headHologramOpt = HologramLib.getManager().flatMap(m -> m.getHologram(headHologramId));

        if (textHologramOpt.isEmpty() || headHologramOpt.isEmpty() ||
                !(textHologramOpt.get() instanceof TextHologram textHologram) ||
                !(headHologramOpt.get() instanceof ItemHologram headHologram)) {
            Bukkit.getLogger().log(Level.WARNING, "Unable to load leaderboard " + id +
                    ": Hologram components not found or of wrong type");
            return;
        }

        LeaderboardHologram.LeaderboardOptions.LeaderboardOptionsBuilder optionsBuilder =
                LeaderboardHologram.LeaderboardOptions.builder();

        optionsBuilder.title(config.getString(path + ".options.title", "Leaderboard"));
        optionsBuilder.scale((float) config.getDouble(path + ".options.scale", 1.0));
        optionsBuilder.topPlayerHead(config.getBoolean(path + ".options.topPlayerHead", true));
        optionsBuilder.showEmptyPlaces(config.getBoolean(path + ".options.showEmptyPlaces", false));
        optionsBuilder.maxDisplayEntries(config.getInt(path + ".options.maxDisplayEntries", 10));
        optionsBuilder.suffix(config.getString(path + ".options.suffix", ""));
        optionsBuilder.defaultPlaceFormat(config.getString(path + ".options.defaultPlaceFormat",
                "<color:#ffb486><bold>{place}. </bold>{name}</color> <gray>{score}</gray> <white>{suffix}</white>"));
        optionsBuilder.titleFormat(config.getString(path + ".options.titleFormat",
                "<gradient:#ff6000:#ffa42a>▛▀▀▀▀ {title} ▀▀▀▀▜</gradient>"));
        optionsBuilder.footerFormat(config.getString(path + ".options.footerFormat",
                "<color:#ff6000>▙▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▟</color>"));

        ConfigurationSection placeFormatsSection = config.getConfigurationSection(path + ".options.placeFormats");
        if (placeFormatsSection != null) {
            String[] placeFormats = new String[placeFormatsSection.getKeys(false).size()];
            for (String key : placeFormatsSection.getKeys(false)) {
                int index = Integer.parseInt(key);
                placeFormats[index] = placeFormatsSection.getString(key);
            }
            optionsBuilder.placeFormats(placeFormats);
        }

        LeaderboardHologram.LeaderboardOptions options = optionsBuilder.build();


        try {
            LeaderboardHologram leaderboard = new LeaderboardHologram(options);

            java.lang.reflect.Field textHologramField = LeaderboardHologram.class.getDeclaredField("textHologram");
            java.lang.reflect.Field firstPlaceHeadField = LeaderboardHologram.class.getDeclaredField("firstPlaceHead");

            textHologramField.setAccessible(true);
            firstPlaceHeadField.setAccessible(true);

            textHologramField.set(leaderboard, textHologram);
            firstPlaceHeadField.set(leaderboard, headHologram);

            Map<Integer, String> emptyData = new HashMap<>();
            leaderboard.updateLeaderboard(emptyData, options);

        } catch (Exception e) {
            Bukkit.getLogger().log(Level.SEVERE, "Failed to reconstruct leaderboard: " + id, e);
        }
    }

    private void applyCommonProperties(Hologram<?> hologram, String path) {
        float scaleX = (float) config.getDouble(path + ".scale.x", 1.0);
        float scaleY = (float) config.getDouble(path + ".scale.y", 1.0);
        float scaleZ = (float) config.getDouble(path + ".scale.z", 1.0);
        hologram.setScale(scaleX, scaleY, scaleZ);

        float translationX = (float) config.getDouble(path + ".translation.x", 0.0);
        float translationY = (float) config.getDouble(path + ".translation.y", 0.0);
        float translationZ = (float) config.getDouble(path + ".translation.z", 0.0);
        hologram.setTranslation(translationX, translationY, translationZ);

        String billboardStr = config.getString(path + ".billboard", "CENTER");
        hologram.setBillboard(Display.Billboard.valueOf(billboardStr));

        hologram.setTeleportDuration(config.getInt(path + ".teleportDuration", 10));
        hologram.setInterpolationDurationTransformation(
                config.getInt(path + ".interpolationDurationTransformation", 10));
        hologram.setViewRange(config.getDouble(path + ".viewRange", 1.0));
        hologram.setUpdateTaskPeriod(config.getLong(path + ".updateTaskPeriod", 20));
        hologram.setMaxPlayerRenderDistanceSquared(
                config.getDouble(path + ".maxPlayerRenderDistanceSquared", 62500));
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
    }
}