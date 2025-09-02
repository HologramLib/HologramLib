package com.maximde.hologramlib.hologram;

import com.maximde.hologramlib.hologram.custom.LeaderboardHologram;
import com.maximde.hologramlib.hologram.custom.PagedLeaderboard;
import com.maximde.hologramlib.persistence.PersistenceManager;
import com.maximde.hologramlib.utils.BukkitTasks;
import com.maximde.hologramlib.utils.TaskHandle;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@RequiredArgsConstructor
public class HologramManager {

    private final Map<TextHologram, TaskHandle> hologramAnimations = new ConcurrentHashMap<>();
    private final Map<String, Hologram<?>> hologramsMap = new ConcurrentHashMap<>();
    private final Map<Integer, Hologram<?>> entityIdToHologramMap = new ConcurrentHashMap<>();
    private final Map<String, InteractionBox> interactionBoxesById = new ConcurrentHashMap<>();
    private final Map<Integer, InteractionBox> interactionBoxesByEntityId = new ConcurrentHashMap<>();

    private final PersistenceManager persistenceManager;


    public interface Events {
        void onJoin(Player player);
        void onQuit(Player player);
    }

    @Getter
    private final List<Events> eventHandlers = new ArrayList<>();

    public void registerEventHandler(Events eventHandler) {
        eventHandlers.add(eventHandler);
    }

    public void removeEventHandler(Events eventHandler) {
        eventHandlers.remove(eventHandler);
    }

    public boolean interactionBoxExists(String id) {
        return interactionBoxesById.containsKey(id);
    }

    public boolean interactionBoxExists(int entityId) {
        return interactionBoxesByEntityId.containsKey(entityId);
    }

    public Optional<InteractionBox> getInteractionBox(String id) {
        return Optional.ofNullable(interactionBoxesById.get(id));
    }

    public Optional<InteractionBox> getInteractionBoxByEntityId(int entityId) {
        return Optional.ofNullable(interactionBoxesByEntityId.get(entityId));
    }

    public List<InteractionBox> getInteractionBoxes() {
        return new ArrayList<>(interactionBoxesById.values());
    }

    public List<String> getInteractionBoxIds() {
        return new ArrayList<>(interactionBoxesById.keySet());
    }

    public InteractionBox spawn(InteractionBox interactionBox, Location location) {
        return spawn(interactionBox, location, true);
    }

    public InteractionBox spawn(InteractionBox interactionBox, Location location,
                                 boolean ignorePitchYaw) {

        register(interactionBox);

        BukkitTasks.runTask(() -> {
            try {
                interactionBox.getInternalAccess().spawn(location, ignorePitchYaw).update();

            } catch (Exception e) {
                Bukkit.getLogger().warning("Error spawning interaction box: " + interactionBox.getId());
                e.printStackTrace();
            }
        });

        return interactionBox;
    }

    public boolean register(InteractionBox interactionBox) {
        if (interactionBox == null) return false;

        String id = interactionBox.getId();
        int entityId = interactionBox.getEntityID();

        if (interactionBoxesById.containsKey(id)) {
            Bukkit.getLogger().severe("InteractionBox ID conflict: " + id);
            return false;
        }

        interactionBoxesById.put(id, interactionBox);
        interactionBoxesByEntityId.put(entityId, interactionBox);
        return true;
    }

    public boolean removeInteractionBox(InteractionBox interactionBox) {
        return interactionBox != null && removeInteractionBox(interactionBox.getId());
    }

    public boolean removeInteractionBox(String id) {
        InteractionBox interactionBox = interactionBoxesById.remove(id);
        if (interactionBox == null) return false;

        int entityId = interactionBox.getEntityID();
        interactionBoxesByEntityId.remove(entityId);

        interactionBox.getInternalAccess().kill();


        return true;
    }

    public void removeAllInteractionBoxes() {
        interactionBoxesById.values().forEach(box -> {
            box.getInternalAccess().kill();
        });

        interactionBoxesById.clear();
        interactionBoxesByEntityId.clear();
    }

    /**
     * Spawns a PagedLeaderboard at the specified location with persistence option
     */
    public PagedLeaderboard spawn(PagedLeaderboard pagedLeaderboard, Location location) {

        registerEventHandler(pagedLeaderboard);

        for (LeaderboardHologram page : pagedLeaderboard.getPages()) {
            page.setFixedRotation();
            spawn(page, location);
        }

        spawn(pagedLeaderboard.getLeftArrow(), location);
        spawn(pagedLeaderboard.getRightArrow(), location);

        spawn(pagedLeaderboard.getLeftInteraction(), location);
        spawn(pagedLeaderboard.getRightInteraction(), location);

        BukkitTasks.runTask(() -> {
            try {
                pagedLeaderboard.init(location);
            } catch (Exception e) {
                removeEventHandler(pagedLeaderboard);
                Bukkit.getLogger().warning("Error spawning PagedLeaderboard with id: " + pagedLeaderboard.getBaseId());
                e.printStackTrace();
            }
        });

        return pagedLeaderboard;
    }


    /**
     * Removes a PagedLeaderboard and all its components with persistence option
     */
    public boolean remove(PagedLeaderboard pagedLeaderboard) {
        if (pagedLeaderboard == null || !pagedLeaderboard.isSpawned()) {
            return false;
        }

        removeEventHandler(pagedLeaderboard);

        boolean success = true;

        for (LeaderboardHologram page : pagedLeaderboard.getPages()) {
            success &= remove(page);
        }

        success &= remove(pagedLeaderboard.getLeftArrow());
        success &= remove(pagedLeaderboard.getRightArrow());

        success &= removeInteractionBox(pagedLeaderboard.getLeftInteraction());
        success &= removeInteractionBox(pagedLeaderboard.getRightInteraction());

        return success;
    }


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

    public Optional<Hologram<?>> getHologramByEntityId(int entityId) {
        return Optional.ofNullable(entityIdToHologramMap.get(entityId));
    }

    public void spawn(LeaderboardHologram leaderboardHologram, Location location) {
        spawn(leaderboardHologram, location, false, true);
    }

    public void spawn(LeaderboardHologram leaderboardHologram, Location location, boolean persistant, boolean ignorePitchYaw) {
        leaderboardHologram.teleport(location);
        for (TextHologram textHologram : leaderboardHologram.getAllTextHolograms()) {
            spawn(textHologram, textHologram.getLocation(), persistant, ignorePitchYaw);
        }

        if (leaderboardHologram.getFirstPlaceHead() != null) {
            spawn(leaderboardHologram.getFirstPlaceHead(), leaderboardHologram.getFirstPlaceHead().getLocation(), persistant, ignorePitchYaw);
        }
    }

    public <H extends Hologram<H>> H spawn(H hologram, Location location) {
        this.register(hologram);
        BukkitTasks.runTask(() -> hologram.getInternalAccess().spawn(location, true).update());

        return hologram;
    }

    public <H extends Hologram<H>> H spawn(H hologram, Location location, boolean persistent, boolean ignorePitchYaw) {
        this.register(hologram);
        BukkitTasks.runTask(() -> {
            try {
                hologram.getInternalAccess().spawn(location, ignorePitchYaw).update();
                if (persistent && persistenceManager != null) {
                    persistenceManager.saveHologram(hologram);
                }
            } catch (Exception e) {
                Bukkit.getLogger().warning("An error occurred while trying to spawn hologram with id: " + hologram.id);
                e.printStackTrace();
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
        entityIdToHologramMap.put(hologram.getEntityID(), hologram);
        return true;
    }

    public boolean remove(Hologram<?> hologram, boolean removePersistence) {
        return hologram != null && remove(hologram.getId(), removePersistence);
    }

    public boolean remove(String id, boolean removePersistence) {
        Hologram<?> hologram = hologramsMap.remove(id);
        if (hologram != null) {
            entityIdToHologramMap.remove(hologram.getEntityID());
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
        entityIdToHologramMap.clear();
    }

    public void removeAll() {
        removeAll(false);
    }

    public boolean remove(LeaderboardHologram leaderboardHologram, boolean removePersistence) {
        boolean success = true;

        for (TextHologram textHologram : leaderboardHologram.getAllTextHolograms()) {
            success &= remove(textHologram, removePersistence);
        }

        if (leaderboardHologram.getFirstPlaceHead() != null) {
            success &= remove(leaderboardHologram.getFirstPlaceHead(), removePersistence);
        }

        return success;
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
        return this.spawn(source.copy(id), source.getLocation(), persistent, false);
    }

    public <H extends Hologram<H>> Hologram<H> copyHologram(H source) {
        return this.spawn(source.copy(), source.getLocation());
    }

    public <H extends Hologram<H>> Hologram<H> copyHologram(H source, boolean persistent) {
        return this.spawn(source.copy(), source.getLocation(), persistent, false);
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
