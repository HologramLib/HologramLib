package me.tofaa.entitylib.spigot;

import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import me.tofaa.entitylib.EntityIdProvider;
import me.tofaa.entitylib.Platform;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class SpigotEntityIdProvider implements EntityIdProvider {

    private final Platform<JavaPlugin> platform;
    private final Supplier<Integer> entityIdSupplier;
    private final AtomicInteger fallbackCounter = new AtomicInteger(1_000_000);

    public SpigotEntityIdProvider(final @NotNull Platform<JavaPlugin> platform) {
        this.platform = platform;
        this.entityIdSupplier = detectIdSupplier();
    }

    @Override
    public int provide(@NotNull UUID entityUUID, @NotNull EntityType entityType) {
        return entityIdSupplier.get();
    }

    private Supplier<Integer> detectIdSupplier() {
        final ServerVersion serverVersion = platform.getAPI().getPacketEvents().getServerManager().getVersion();

        if (isPaper() && serverVersion.isNewerThanOrEquals(ServerVersion.V_1_16)) {
            Supplier<Integer> paperSupplier = resolvePaperSupplier();
            if (paperSupplier != null) {
                return paperSupplier;
            }
        }

        Supplier<Integer> nmsSupplier = resolveNmsSupplier(serverVersion);
        if (nmsSupplier != null) {
            return nmsSupplier;
        }

        return fallbackCounter::incrementAndGet;
    }

    private Supplier<Integer> resolvePaperSupplier() {
        try {
            Object unsafe = Bukkit.getUnsafe();
            if (unsafe == null) {
                return null;
            }

            try {
                Method nextWithWorld = unsafe.getClass().getMethod("nextEntityId", World.class);
                nextWithWorld.setAccessible(true);
                return () -> {
                    try {
                        List<World> worlds = Bukkit.getWorlds();
                        World world = worlds.isEmpty() ? null : worlds.get(0);
                        return (int) nextWithWorld.invoke(unsafe, world);
                    } catch (Exception e) {
                        return fallbackCounter.incrementAndGet();
                    }
                };
            } catch (NoSuchMethodException ignored) {
            }

            try {
                Method nextNoArgs = unsafe.getClass().getMethod("nextEntityId");
                nextNoArgs.setAccessible(true);
                return () -> {
                    try {
                        return (int) nextNoArgs.invoke(unsafe);
                    } catch (Exception e) {
                        return fallbackCounter.incrementAndGet();
                    }
                };
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Supplier<Integer> resolveNmsSupplier(ServerVersion serverVersion) {
        try {
            Class<?> serverLevelClass = Class.forName("net.minecraft.server.level.ServerLevel");
            Field counterField = getAtomicIntegerField(serverLevelClass, "ENTITY_COUNTER");
            if (counterField != null) {
                counterField.setAccessible(true);
                AtomicInteger counter = (AtomicInteger) counterField.get(null);
                if (counter != null) {
                    return counter::incrementAndGet;
                }
            }
        } catch (Throwable ignored) {
        }

        try {
            Class<?> entityClass = getEntityClass(serverVersion);
            if (entityClass != null) {
                if (serverVersion.isNewerThanOrEquals(ServerVersion.V_1_14)) {
                    Field entityAtomicField = getAtomicIntegerField(entityClass, "ENTITY_COUNTER", "entityCount", "d", "c");
                    if (entityAtomicField != null) {
                        entityAtomicField.setAccessible(true);
                        AtomicInteger counter = (AtomicInteger) entityAtomicField.get(null);
                        if (counter != null) {
                            return counter::incrementAndGet;
                        }
                    }
                }

                Field entityLegacyField = getField(entityClass, "entityCount");
                if (entityLegacyField != null && entityLegacyField.getType() == int.class) {
                    entityLegacyField.setAccessible(true);
                    return () -> {
                        try {
                            int entityId = entityLegacyField.getInt(null);
                            entityLegacyField.setInt(null, entityId + 1);
                            return entityId;
                        } catch (Exception exception) {
                            return fallbackCounter.incrementAndGet();
                        }
                    };
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private Class<?> getEntityClass(ServerVersion serverVersion) {
        boolean isFlattened = serverVersion.isNewerThanOrEquals(ServerVersion.V_1_17);
        try {
            if (isFlattened) {
                return Class.forName("net.minecraft.world.entity.Entity");
            } else {
                String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
                return Class.forName("net.minecraft.server." + version + ".Entity");
            }
        } catch (ClassNotFoundException ignored) {
            try {
                return Class.forName("net.minecraft.world.entity.Entity");
            } catch (ClassNotFoundException e) {
                return null;
            }
        }
    }

    private static Field getAtomicIntegerField(Class<?> clazz, String... possibleNames) {
        for (String name : possibleNames) {
            try {
                Field f = clazz.getDeclaredField(name);
                if (AtomicInteger.class.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) {
                    return f;
                }
            } catch (NoSuchFieldException ignored) {
            }
        }
        for (Field f : clazz.getDeclaredFields()) {
            if (AtomicInteger.class.isAssignableFrom(f.getType()) && Modifier.isStatic(f.getModifiers())) {
                return f;
            }
        }
        return null;
    }

    private static Field getField(final Class<?> clazz, final String... possibleNames) {
        for (final String name : possibleNames) {
            try {
                return clazz.getDeclaredField(name);
            } catch (final NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static boolean isPaper() {
        return Stream.of(
                "com.destroystokyo.paper.PaperConfig",
                "io.papermc.paper.configuration.Configuration"
        ).anyMatch(SpigotEntityIdProvider::hasClass);
    }

    private static boolean hasClass(final String className) {
        try {
            Class.forName(className);
            return true;
        } catch (final ClassNotFoundException ignored) {
            return false;
        }
    }
}
