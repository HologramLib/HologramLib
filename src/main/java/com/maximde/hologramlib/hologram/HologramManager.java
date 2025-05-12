package com.maximde.hologramlib.hologram;

import com.maximde.hologramlib.persistence.PersistenceManager;
import com.maximde.hologramlib.utils.BukkitTasks;
import com.maximde.hologramlib.utils.TaskHandle;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;


@RequiredArgsConstructor
public class HologramManager {

    private final Map<TextHologram, TaskHandle> hologramAnimations = new ConcurrentHashMap<>();
    private final Map<String, Hologram<?>> hologramsMap = new ConcurrentHashMap<>();

    private final PersistenceManager persistenceManager;

    @Deprecated
    public Map<String, Hologram<?>> getHologramsMap() {
        return this.hologramsMap;
    }

    @Deprecated
    public Map<TextHologram, TaskHandle> getHologramAnimations() {
        return this.hologramAnimations;
    }

    public boolean hologramExists(String id) {
        return hologramsMap.containsKey(id);
    }

    public boolean hologramExists(Hologram<?> hologram) {
        return hologramsMap.containsValue(hologram);
    }

    public List<Hologram<?>> getHolograms() {
        return new ArrayList<>(hologramsMap.values());
    }

    public List<String> getHologramIds() {
        return new ArrayList<>(hologramsMap.keySet());
    }

    public Optional<Hologram<?>> getHologram(String id) {
        return Optional.ofNullable(hologramsMap.get(id));
    }

    public LeaderboardHologram generateLeaderboard(Location location, Map<Integer, String> leaderboardData, boolean persistant) {
        return generateLeaderboard(location, leaderboardData, LeaderboardHologram.LeaderboardOptions.builder().build(), persistant);
    }

    public LeaderboardHologram generateLeaderboard(Location location, Map<Integer, String> leaderboardData) {
        return generateLeaderboard(location, leaderboardData, LeaderboardHologram.LeaderboardOptions.builder().build(), false);
    }

    public LeaderboardHologram generateLeaderboard(Location location, Map<Integer, String> leaderboardData, LeaderboardHologram.LeaderboardOptions options) {
        return generateLeaderboard(location, leaderboardData, options, false);
    }

    public LeaderboardHologram generateLeaderboard(Location location, Map<Integer, String> leaderboardData, LeaderboardHologram.LeaderboardOptions options, boolean persistant) {
        LeaderboardHologram leaderboardHologram = new LeaderboardHologram(options);
        updateLeaderboard(leaderboardHologram, leaderboardData, options);
        spawnElements(leaderboardHologram, location, persistant);
        return leaderboardHologram;
    }

    private void spawnElements(LeaderboardHologram leaderboardHologram, Location location, boolean persistant) {
        spawn(leaderboardHologram.getTextHologram(), location, persistant);
        spawn(leaderboardHologram.getFirstPlaceHead(), location, persistant);
    }
    public void updateLeaderboard(LeaderboardHologram leaderboardHologram, Map<Integer, String> leaderboardData, LeaderboardHologram.LeaderboardOptions options) {
        leaderboardHologram.updateLeaderboard(leaderboardData, options);
    }

    public <H extends Hologram<H>> H spawn(H hologram, Location location) {
        BukkitTasks.runTask(() -> hologram.getInternalAccess().spawn(location).update());

        this.register(hologram);
        return hologram;
    }

    public <H extends Hologram<H>> H spawn(H hologram, Location location, boolean persistent) {
        BukkitTasks.runTask(() -> {
            hologram.getInternalAccess().spawn(location).update();
            this.register(hologram);
            if (persistent && persistenceManager != null) {
                persistenceManager.saveHologram(hologram);
            }
        });
        return hologram;
    }

    public void attach(Hologram<?> hologram, int entityID) {
        hologram.attach(entityID);
    }

    public <H extends Hologram<H>> boolean register(H hologram) {
        if (hologram == null) {
            return false;
        }
        if (hologramsMap.containsKey(hologram.getId())) {
            Bukkit.getLogger().severe("Error: Hologram with ID " + hologram.getId() + " is already registered.");
            return false;
        }
        hologramsMap.put(hologram.getId(), hologram);
        return true;
    }

    public boolean remove(Hologram<?> hologram, boolean removePersistence) {
        return hologram != null && remove(hologram.getId(), removePersistence);
    }

    public boolean remove(String id, boolean removePersistence) {
        Hologram<?> hologram = hologramsMap.remove(id);
        if (hologram != null) {
            if (hologram instanceof TextHologram textHologram) cancelAnimation(textHologram);
            hologram.getInternalAccess().kill();

            if (persistenceManager != null) {
                if (removePersistence && persistenceManager.getPersistentHolograms().contains(id)) {
                    persistenceManager.removeHologram(id);
                } else if (persistenceManager.getPersistentHolograms().contains(id)) {
                    persistenceManager.saveHologram(hologram);
                }
            }

            return true;
        }
        return false;
    }

    public boolean remove(Hologram<?> hologram) {
        return remove(hologram, false);
    }

    public boolean remove(String id) {
        return remove(id, false);
    }

    public void removeAll(boolean removePersistence) {
        hologramsMap.values().forEach(hologram -> {
            if (hologram instanceof TextHologram textHologram) cancelAnimation(textHologram);
            hologram.getInternalAccess().kill();

            if (!removePersistence && persistenceManager != null &&
                    persistenceManager.getPersistentHolograms().contains(hologram.getId())) {
                persistenceManager.saveHologram(hologram);
            }
        });

        if (removePersistence && persistenceManager != null) {
            for (String id : new ArrayList<>(persistenceManager.getPersistentHolograms())) {
                persistenceManager.removeHologram(id);
            }
        }

        hologramsMap.clear();
    }

    public void removeAll() {
        removeAll(false);
    }

    public boolean remove(LeaderboardHologram leaderboardHologram, boolean removePersistence) {
        return remove(leaderboardHologram.getTextHologram(), removePersistence) &&
                remove(leaderboardHologram.getFirstPlaceHead(), removePersistence);
    }

    public boolean remove(LeaderboardHologram leaderboardHologram) {
        return remove(leaderboardHologram, false);
    }

    public void applyAnimation(TextHologram hologram, TextAnimation textAnimation) {
        cancelAnimation(hologram);
        hologramAnimations.put(hologram, animateHologram(hologram, textAnimation));
    }

    public void cancelAnimation(TextHologram hologram) {
        Optional.ofNullable(hologramAnimations.remove(hologram)).ifPresent(TaskHandle::cancel);
    }

    private TaskHandle animateHologram(TextHologram hologram, TextAnimation textAnimation) {
        return BukkitTasks.runTaskTimerAsync(() -> {
            if (textAnimation.getTextFrames().isEmpty()) return;
            hologram.setMiniMessageText(textAnimation.getTextFrames().get(0));
            hologram.update();
            Collections.rotate(textAnimation.getTextFrames(), -1);
        }, textAnimation.getDelay(), textAnimation.getSpeed());
    }

    public void ifHologramExists(String id, Consumer<Hologram<?>> action) {
        Optional.ofNullable(hologramsMap.get(id)).ifPresent(action);
    }

    public boolean updateHologramIfExists(String id, Consumer<Hologram<?>> updateAction) {
        Hologram<?> hologram = hologramsMap.get(id);
        if (hologram != null) {
            updateAction.accept(hologram);
            return true;
        }
        return false;
    }

    public <H extends Hologram<H>> Hologram<H> copyHologram(H source, String id) {
        return this.spawn(source.copy(id), source.getLocation());
    }

    public <H extends Hologram<H>> Hologram<H> copyHologram(H source, String id, boolean persistent) {
        return this.spawn(source.copy(id), source.getLocation(), persistent);
    }

    public <H extends Hologram<H>> Hologram<H> copyHologram(H source) {
        return this.spawn(source.copy(), source.getLocation());
    }

    public <H extends Hologram<H>> Hologram<H> copyHologram(H source, boolean persistent) {
        return this.spawn(source.copy(), source.getLocation(), persistent);
    }

    /**
     * Makes an existing hologram persistent so it will be saved and loaded on server restart.
     *
     * @param id The ID of the hologram to make persistent
     * @return true if the hologram was found and made persistent, false otherwise
     */
    public boolean makePersistent(String id) {
        if (persistenceManager != null && hologramsMap.containsKey(id)) {
            persistenceManager.saveHologram(hologramsMap.get(id));
            return true;
        }
        return false;
    }

    /**
     * Removes persistence from a hologram so it will no longer be saved.
     * The hologram will remain active until the server restarts.
     *
     * @param id The ID of the hologram to remove persistence from
     * @return true if the hologram was found and persistence was removed, false otherwise
     */
    public boolean removePersistence(String id) {
        if (persistenceManager != null && persistenceManager.getPersistentHolograms().contains(id)) {
            persistenceManager.removeHologram(id);
            return true;
        }
        return false;
    }
}
