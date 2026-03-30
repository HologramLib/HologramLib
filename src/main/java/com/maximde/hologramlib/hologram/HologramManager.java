package com.maximde.hologramlib.hologram;

import com.maximde.hologramlib.hologram.custom.LeaderboardHologram;
import com.maximde.hologramlib.hologram.custom.PagedLeaderboard;
import com.maximde.hologramlib.hologram.custom.SwitchableLeaderboard;
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
        if(interactionBox == null) return interactionBox;
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
        spawn(leaderboardHologram, location, true);
    }

    public void spawn(LeaderboardHologram leaderboardHologram, Location location, boolean ignorePitchYaw) {
        leaderboardHologram.spawn(location, ignorePitchYaw);
        for (TextHologram textHologram : leaderboardHologram.getAllTextHolograms()) {
            register(textHologram);
        }
        if (leaderboardHologram.getFirstPlaceHead() != null) {
            register(leaderboardHologram.getFirstPlaceHead());
        }
    }

    public <H extends Hologram<H>> H spawn(H hologram, Location location) {
        this.register(hologram);
        BukkitTasks.runTask(() -> {
            hologram.getInternalAccess().spawn(location, true).update();
            spawn(hologram.getInteractionBox(), location);
        });

        return hologram;
    }

    public <H extends Hologram<H>> H spawn(H hologram, Location location,  boolean ignorePitchYaw) {
        this.register(hologram);
        BukkitTasks.runTask(() -> {
            try {
                hologram.getInternalAccess().spawn(location, ignorePitchYaw).update();
                spawn(hologram.getInteractionBox(), location, ignorePitchYaw);
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

    public boolean remove(Hologram<?> hologram) {
        return hologram != null && remove(hologram.getId());
    }

    public boolean remove(String id) {
        Hologram<?> hologram = hologramsMap.remove(id);
        if (hologram != null) {
            entityIdToHologramMap.remove(hologram.getEntityID());
            if (hologram instanceof TextHologram textHologram) cancelAnimation(textHologram);
            hologram.getInternalAccess().kill();
            removeInteractionBox(hologram.getInteractionBox());

            return true;
        }
        return false;
    }

    public void removeAll() {
        hologramsMap.values().forEach(hologram -> {
            if (hologram instanceof TextHologram textHologram) cancelAnimation(textHologram);
            hologram.getInternalAccess().kill();
        });


        hologramsMap.clear();
        entityIdToHologramMap.clear();
    }

    public boolean remove(LeaderboardHologram leaderboardHologram) {
        boolean success = true;

        for (TextHologram textHologram : leaderboardHologram.getAllTextHolograms()) {
            success &= remove(textHologram);
        }

        if (leaderboardHologram.getFirstPlaceHead() != null) {
            success &= remove(leaderboardHologram.getFirstPlaceHead());
        }

        return success;
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
        return this.spawn(source.copy(id), source.getLocation(), false);
    }

    public <H extends Hologram<H>> Hologram<H> copyHologram(H source) {
        return this.spawn(source.copy(), source.getLocation());
    }

    public <H extends Hologram<H>> Hologram<H> copyHologram(H source, boolean persistent) {
        return this.spawn(source.copy(), source.getLocation(), false);
    }

    /**
     * Spawns a SwitchableLeaderboard at the specified location
     * This method should be added to HologramManager
     *
     * @param switchableLeaderboard The switchable leaderboard to spawn
     * @param location The location to spawn at
     * @return The spawned SwitchableLeaderboard
     */
    public SwitchableLeaderboard spawn(HologramManager manager, SwitchableLeaderboard switchableLeaderboard, Location location) {

        manager.registerEventHandler(switchableLeaderboard);

        for (SwitchableLeaderboard.StatMode statMode : switchableLeaderboard.getStatModes().values()) {
            for (LeaderboardHologram page : statMode.getPages()) {
                page.setFixedRotation();
                manager.spawn(page, location);
            }
        }

        manager.spawn(switchableLeaderboard.getLeftArrow(), location);
        manager.spawn(switchableLeaderboard.getRightArrow(), location);
        manager.spawn(switchableLeaderboard.getLeftInteraction(), location);
        manager.spawn(switchableLeaderboard.getRightInteraction(), location);

        for (SwitchableLeaderboard.StateButton button : switchableLeaderboard.getStateButtons()) {
            manager.spawn(button.getHologram(), location);
            manager.spawn(button.getInteraction(), location);
        }

        BukkitTasks.runTask(() -> {
            try {
                switchableLeaderboard.init(location);
            } catch (Exception e) {
                manager.removeEventHandler(switchableLeaderboard);
                Bukkit.getLogger().warning("Error spawning SwitchableLeaderboard with id: " + switchableLeaderboard.getBaseId());
                e.printStackTrace();
            }
        });

        return switchableLeaderboard;
    }

    /**
     * Removes a SwitchableLeaderboard and all its components
     * This method should be added to HologramManager
     *
     * @param manager The HologramManager instance
     * @param switchableLeaderboard The switchable leaderboard to remove
     * @return true if successfully removed, false otherwise
     */
    public boolean remove(HologramManager manager, SwitchableLeaderboard switchableLeaderboard) {
        if (switchableLeaderboard == null || !switchableLeaderboard.isSpawned()) {
            return false;
        }

        manager.removeEventHandler(switchableLeaderboard);

        boolean success = true;

        for (SwitchableLeaderboard.StatMode statMode : switchableLeaderboard.getStatModes().values()) {
            for (LeaderboardHologram page : statMode.getPages()) {
                success &= manager.remove(page);
            }
        }

        success &= manager.remove(switchableLeaderboard.getLeftArrow());
        success &= manager.remove(switchableLeaderboard.getRightArrow());
        success &= manager.removeInteractionBox(switchableLeaderboard.getLeftInteraction());
        success &= manager.removeInteractionBox(switchableLeaderboard.getRightInteraction());


        for (SwitchableLeaderboard.StateButton button : switchableLeaderboard.getStateButtons()) {
            success &= manager.remove(button.getHologram());
            success &= manager.removeInteractionBox(button.getInteraction());
        }

        return success;
    }

}
