package com.maximde.hologramlib;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.maximde.hologramlib.bstats.Metrics;
import com.maximde.hologramlib.hologram.HologramManager;
import com.maximde.hologramlib.persistence.PersistenceManager;
import com.maximde.hologramlib.utils.BukkitTasks;
import com.maximde.hologramlib.utils.ItemsAdderHolder;
import com.maximde.hologramlib.utils.ReplaceText;
import com.maximjsx.addonlib.core.AddonLib;
import com.maximjsx.addonlib.util.Logger;
import com.tcoded.folialib.FoliaLib;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import lombok.Getter;
import me.tofaa.entitylib.APIConfig;
import me.tofaa.entitylib.EntityLib;
import me.tofaa.entitylib.spigot.SpigotEntityLibPlatform;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;


public abstract class HologramLib {

    @Getter
    private static ReplaceText replaceText;

    @Getter
    private static PlayerManager playerManager;

    private static HologramManager hologramManager;

    private static JavaPlugin plugin;

    private static boolean initialized = false;

    @Getter
    private static PersistenceManager persistenceManager;

    public static Optional<HologramManager> getManager() {
        init();
        return Optional.ofNullable(hologramManager)
                .or(() -> {
                    Bukkit.getLogger().log(Level.SEVERE,
                            "HologramLib has not been initialized yet. " +
                                    "Ensure 'HologramLib' is included as a dependency in your plugin.yml.");
                    return Optional.empty();
                });
    }

    public static void onLoad(JavaPlugin javaPlugin) {
        if (plugin != null) return;
        plugin = javaPlugin;
        Optional.ofNullable(SpigotPacketEventsBuilder.build(plugin))
                .ifPresentOrElse(
                        PacketEvents::setAPI,
                        () -> plugin.getLogger().severe("Failed to build PacketEvents API")
                );

        PacketEvents.getAPI().load();
    }

    public static void init(PluginCommand command) {
        init();
        Objects.requireNonNull(command).setExecutor(new Command(new AddonLib((logLevel, message) -> Bukkit.getLogger().log(toJavaUtilLevel(logLevel), message), plugin.getDataFolder(), plugin.getDescription().getVersion())));
    }

    public static void init() {
        if(plugin == null) {
            Bukkit.getLogger().log(Level.SEVERE,
                    "Failed to init HologramLib! HologramLib#onLoad(JavaPlugin) was not called in onLoad() main class.");
            Bukkit.getLogger().log(Level.SEVERE,
                    "If you are not shading HologramLib, add depends: HologramLib to your plugin.yml");
            return;
        }

        if(initialized) {
            Bukkit.getLogger().log(Level.INFO,
                    "Tried to initialize HologramLib a second time.");
            return;
        }

        initialized = true;

        try {
            initializePacketEvents();
            initializeEntityLib();
            initializeManagers();
            initializeMetrics();
            initializeReplaceText();

            FoliaLib foliaLib = new FoliaLib(plugin);
            BukkitTasks.setPlugin(plugin);
            BukkitTasks.setFoliaLib(foliaLib);

            persistenceManager = new PersistenceManager();
            hologramManager = new HologramManager(persistenceManager);
            persistenceManager.loadHolograms();

            new AddonLib((logLevel, message) -> Bukkit.getLogger().log(toJavaUtilLevel(logLevel), message), plugin.getDataFolder(), plugin.getDescription().getVersion())
                    .setEnabledAddons(new String[]{"Commands"})
                    .init();

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to enable HologramLib", e);
            Bukkit.getPluginManager().disablePlugin(plugin);
        }
    }

    public static void onDisable() {
        savePersistentHolograms();
    }

    public static void savePersistentHolograms() {
        if (persistenceManager != null && hologramManager != null) {
            Bukkit.getLogger().log(Level.INFO, "Saving all persistent holograms before shutdown...");
            for (String id : persistenceManager.getPersistentHolograms()) {
                hologramManager.getHologram(id).ifPresent(persistenceManager::saveHologram);
            }
            Bukkit.getLogger().log(Level.INFO, "Persistent holograms saved successfully.");
        }
    }

    public static Level toJavaUtilLevel(Logger.LogLevel logLevel) {
        return switch (logLevel) {
            case INFO -> Level.INFO;
            case SUCCESS -> Level.FINE;
            case WARNING -> Level.WARNING;
            case ERROR -> Level.SEVERE;
        };
    }

    private static void initializePacketEvents() {
        PacketEvents.getAPI().init();
    }

    private static void initializeEntityLib() {
        SpigotEntityLibPlatform platform = new SpigotEntityLibPlatform(plugin);
        APIConfig config = new APIConfig(PacketEvents.getAPI())
                .usePlatformLogger();
        EntityLib.init(platform, config);
    }

    private static void initializeManagers() {
        playerManager = PacketEvents.getAPI().getPlayerManager();
        plugin.getLogger().log(Level.INFO, "Initialized HologramLib Manager!");
    }

    private static void initializeMetrics() {
        new Metrics(plugin, 19375);
    }

    private static void initializeReplaceText() {
        replaceText = createReplaceTextInstance()
                .orElse(text -> text);
    }

    private static Optional<ReplaceText> createReplaceTextInstance() {
        try {
            return Optional.of(new ItemsAdderHolder());
        } catch (ClassNotFoundException e) {
            plugin.getLogger().log(Level.INFO,"ItemsAdder not found. Using default text replacement.");
            return Optional.empty();
        }
    }

    public static JavaPlugin getPlugin() {
        if (plugin == null) {
            throw new IllegalStateException("HologramLib has not been initialized");
        }
        return plugin;
    }
}