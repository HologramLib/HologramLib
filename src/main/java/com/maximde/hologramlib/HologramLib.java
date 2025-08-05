package com.maximde.hologramlib;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.maximde.hologramlib.bstats.Metrics;
import com.maximde.hologramlib.hologram.HologramManager;
import com.maximde.hologramlib.hologram.PassengerManager;
import com.maximde.hologramlib.hook.PlaceholderAPIHook;
import com.maximde.hologramlib.listener.InteractionPacketListener;
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
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Interaction;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
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
    private static boolean loading = false;

    @Getter
    private static PersistenceManager persistenceManager;

    public static Optional<HologramManager> getManager() {
        init();
        return Optional.ofNullable(hologramManager)
                .or(() -> {
                    Bukkit.getLogger().log(Level.WARNING,
                            "HologramLib#getManager() couldn't provide a valid instance! " +
                                    "The plugin was not fully initialized yet.");
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

    private static void registerCommand(org.bukkit.command.Command command) {
        if(plugin == null) return;
        try {
            final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            bukkitCommandMap.setAccessible(true);
            CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());
            commandMap.register(plugin.getDescription().getName(), command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        init(true);
    }

    public static void init(boolean registerCommand) {
        if(loading || initialized) return;

       if(plugin == null) {
            Bukkit.getLogger().log(Level.SEVERE,
                    "Failed to init HologramLib! HologramLib#onLoad(JavaPlugin) was not called in onLoad() main class.");
            Bukkit.getLogger().log(Level.SEVERE,
                    "If you are not shading HologramLib, add depends: HologramLib to your plugin.yml");
            return;
        }

       if(!plugin.isEnabled()) {
           Bukkit.getLogger().log(Level.SEVERE,
                   "Failed to init HologramLib! The plugin instance which is used by HologramLib has not been initialized (" + plugin.getName() + ") yet!");

           Bukkit.getLogger().log(Level.SEVERE,
                   "If you are not shading HologramLib, add depends: HologramLib to your plugin.yml");
           return;
       }

        loading = true;

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
            PacketEvents.getAPI().getEventManager().registerListener(new InteractionPacketListener(hologramManager),
                    PacketListenerPriority.LOW);
            persistenceManager.loadHolograms();

            PluginManager pluginManager = Bukkit.getPluginManager();

            Plugin placeholderAPIPlugin = pluginManager.getPlugin("PlaceholderAPI");
            if (placeholderAPIPlugin != null && placeholderAPIPlugin.isEnabled()) {
                plugin.getLogger().log(Level.INFO, "PlaceholderAPI found! Initializing hook...");
                new PlaceholderAPIHook(PacketEvents.getAPI());
            } else {
                plugin.getLogger().log(Level.INFO, "PlaceholderAPI not found or not enabled. PlaceholderAPI support will be disabled.");
            }

            AddonLib addonLib = new AddonLib((logLevel, message) -> Bukkit.getLogger().log(toJavaUtilLevel(logLevel), message), plugin.getDataFolder(), plugin.getDescription().getVersion());
            addonLib.setEnabledAddons(new String[]{"Commands"})
                    .init();
            if(registerCommand) registerCommand(new HoloCommand(addonLib));


            initialized = true;
            plugin.getLogger().log(Level.INFO, "Successfully initialized!");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to enable HologramLib", e);
        } finally {
            loading = false;
        }
    }

    public static void onDisable() {
        try {
            savePersistentHolograms();
            hologramManager.removeAll();
            hologramManager.removeAllInteractionBoxes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void savePersistentHolograms() {
        if (persistenceManager != null && hologramManager != null) {
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
        PacketEventsAPI<?> packetEventsAPI = PacketEvents.getAPI();
        playerManager = packetEventsAPI.getPlayerManager();
        new PassengerManager(packetEventsAPI);
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
            plugin.getLogger().log(Level.INFO,"Using default text replacement. (ItemsAdder is not installed)");
            return Optional.empty();
        }
    }

    public static JavaPlugin getPlugin() {
        if (plugin == null) {
            throw new IllegalStateException("Tried to access the plugin instance but HologramLib has not been initialized");
        }
        return plugin;
    }
}