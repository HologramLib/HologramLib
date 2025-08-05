package com.maximde.hologramlib.hologram;


import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.wrapper.PacketWrapper;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import com.maximde.hologramlib.HologramLib;
import com.maximde.hologramlib.utils.BukkitTasks;
import com.maximde.hologramlib.utils.TaskHandle;
import com.maximde.hologramlib.utils.Vector3F;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import lombok.Getter;
import lombok.experimental.Accessors;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.other.InteractionMeta;
import me.tofaa.entitylib.wrapper.WrapperEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@ApiStatus.Experimental
public class InteractionBox {

    @Getter
    protected Location location;

    @Getter
    protected boolean dead = true;

    @Getter @Accessors(chain = true)
    protected long updateTaskPeriod = 20L;

    @Getter @Accessors(chain = true)
    protected double maxPlayerRenderDistanceSquared = 62500;

    @Getter @Accessors(chain = true)
    protected float width = 1.0f;

    @Getter @Accessors(chain = true)
    protected float height = 1.0f;

    @Getter @Accessors(chain = true)
    protected boolean responsive = true;

    @Getter
    protected final String id;

    @Getter @Accessors(chain = true)
    protected int entityID;

    /**
     * The render mode determines which players can see the interaction:
     * - NEARBY: Only players within viewing distance
     * - ALL: All players on the server
     * - VIEWER_LIST: Only specific players added as viewers
     * - NONE: Interaction is not visible to any players
     */
    @Getter
    protected final RenderMode renderMode;

    @Getter
    protected final EntityType entityType;

    @Getter
    protected TaskHandle task;

    /**
     * Do not use this if you don't know what you are doing!
     * This interface for accessing specific setters is only for internal methods.
     */
    @Getter
    private final Internal internalAccess;

    protected WrapperEntity entity;

    protected @Nullable Integer attachedEntityId;

    /**
     * Players which will not be automatically added as viewers no matter which render mode
     */
    @Getter
    private final List<Player> blacklistedViewers = new ArrayList<>();

    @FunctionalInterface
    public interface OnInteract {
        void onInteract(Player player);
    }

    private final OnInteract onInteract;

    public interface Internal {
        InteractionBox spawn(Location location, boolean ignorePitchYaw);
        void kill();
        void setLocation(Location location);
    }

    public InteractionBox(String id, OnInteract onInteract) {
        this(id, RenderMode.ALL, onInteract);
    }

    public InteractionBox(String id, RenderMode renderMode, OnInteract onInteract) {
        this.onInteract = onInteract;
        this.entityType = EntityTypes.INTERACTION;
        validateId(id);
        this.entity = new WrapperEntity(entityType);
        this.id = id;
        this.entityID = entity.getEntityId();
        this.renderMode = renderMode;
        this.internalAccess = new InternalSetters();
        startRunnable();
    }

    private void startRunnable() {
        if (task != null) return;
        task = BukkitTasks.runTaskTimerAsync(this::updateAffectedPlayers, 20L, updateTaskPeriod);
    }

    private class InternalSetters implements Internal {

        @Override
        public InteractionBox spawn(Location location, boolean ignoreYawPitch) {
            InteractionBox.this.spawn(location, ignoreYawPitch);
            return InteractionBox.this;
        }

        @Override
        public void kill() {
            InteractionBox.this.kill();
        }

        @Override
        public void setLocation(Location location) {
            InteractionBox.this.setLocation(location);
        }
    }

    private void setLocation(Location location) {
        this.location = location;
    }

    public void show(Player player) {
        this.addToViewerBlacklist(player);
        this.addViewer(player);
    }

    public void hide(Player player) {
        this.removeFromViewerBlacklist(player);
        this.removeViewer(player);
    }

    public void addToViewerBlacklist(Player player) {
        this.blacklistedViewers.add(player);
    }

    public void removeFromViewerBlacklist(Player player) {
        this.blacklistedViewers.remove(player);
    }

    /**
     * Updates the set properties for the entity (shows them to the players).
     * Should be called after making any changes to the interaction object.
     */
    public InteractionBox update() {
        this.updateAffectedPlayers();
        this.applyMeta();
        return this;
    }

    protected void validateId(String id) {
        if (id.contains(" ")) {
            throw new IllegalArgumentException("The interaction ID cannot contain spaces! (" + id + ")");
        }
    }

    /**
     * THIS METHOD WILL BE MADE 'private' SOON!
     * Use HologramManager#remove(HologramInteraction) instead!
     */
    @Deprecated
    public void kill() {
        this.entity.remove();
        this.task.cancel();
        this.dead = true;
    }

    public InteractionBox teleport(Location newLocation) {
        this.location = newLocation;
        this.entity.teleport(SpigotConversionUtil.fromBukkitLocation(newLocation));
        return this;
    }

    protected EntityMeta applyMeta() {
        InteractionMeta meta = (InteractionMeta) this.entity.getEntityMeta();
        meta.setWidth(this.width);
        meta.setHeight(this.height);
        meta.setResponsive(this.responsive);
        return meta;
    }

    /**
     * Updates which players should be able to see this interaction based on the render mode.
     * For ALL mode, adds all online players.
     * For VIEWER_LIST mode, only uses manually added viewers.
     * Removes viewers who are too far away or in different worlds.
     */
    private void updateAffectedPlayers() {
        if(this.dead) return;

        if(renderMode == RenderMode.VIEWER_LIST) {
            if (attachedEntityId != null) {
                Player attachedPlayer = getPlayerByEntityId(attachedEntityId);
                if (attachedPlayer != null && attachedPlayer.isOnline()) {
                    Location playerLocation = attachedPlayer.getLocation();
                    if (playerLocation.getWorld() != null) {
                        this.teleport(playerLocation);
                    }
                }
                Set<UUID> currentViewers = new HashSet<>(this.entity.getViewers());
                currentViewers.forEach(uuid -> {
                    Player player = Bukkit.getPlayer(uuid);
                    if (player != null && player.isOnline()) {
                        sendPacket(new WrapperPlayServerSetPassengers(
                                        attachedEntityId, addElement(PassengerManager.getPassengers(attachedEntityId), this.entityID)),
                                Collections.singletonList(player)
                        );
                    }
                });
            }
            return;
        }

        if(this.location == null) {
            Bukkit.getLogger().log(Level.WARNING, "Tried to update interaction with ID " + this.id + " entity type " + this.entityType.getName().getKey() + ". But the location is not set!");
            return;
        }

        World world = this.location.getWorld();

        if (world != null && (this.renderMode == RenderMode.ALL || this.renderMode == RenderMode.NEARBY || this.renderMode == RenderMode.NOT_ATTACHED_PLAYER)) {
            List<Player> viewersToKeep = world.getPlayers().stream()
                    .filter(Objects::nonNull)
                    .filter(player -> player.isOnline()
                            && !this.blacklistedViewers.contains(player)
                            && Objects.equals(player.getLocation().getWorld(), world)
                            && player.getLocation().distanceSquared(this.location) <= this.maxPlayerRenderDistanceSquared)
                    .toList();

            if (this.renderMode == RenderMode.NOT_ATTACHED_PLAYER && attachedEntityId != null) {
                Player attachedPlayer = getPlayerByEntityId(attachedEntityId);
                if (attachedPlayer != null) {
                    viewersToKeep = viewersToKeep.stream()
                            .filter(player -> !player.equals(attachedPlayer))
                            .toList();
                }
            }

            Set<UUID> viewersToRemove = new HashSet<>(this.entity.getViewers());
            viewersToKeep.forEach(player -> viewersToRemove.remove(player.getUniqueId()));

            viewersToRemove.forEach(this.entity::removeViewer);
            this.addAllViewers(viewersToKeep);
        }
    }

    protected void sendPacket(PacketWrapper<?> packet, List<Player> players) {
        if (this.renderMode == RenderMode.NONE) return;

        players.forEach(player ->
                HologramLib.getPlayerManager().sendPacket(player, packet));
    }

    private void spawn(Location location, boolean ignorePitchYaw) {
        this.location = location;
        if(ignorePitchYaw) {
            this.location.setPitch(0);
            this.location.setYaw(0);
        }
        this.entity.spawn(SpigotConversionUtil.fromBukkitLocation(this.location));
        this.dead = false;
    }

    private void spawn(Location location) {
        this.spawn(location, false);
    }

    /**
     * Attaches this interaction to a player.
     * @param player The player to attach the interaction to
     */
    public void attachToPlayer(Player player) {
        attach(player.getEntityId());
    }

    /**
     * Gets the player associated with the given entity ID.
     * @param entityId The entity ID to search for
     * @return The player if found, null otherwise
     */
    private Player getPlayerByEntityId(int entityId) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getEntityId() == entityId) {
                return player;
            }
        }
        return null;
    }

    /**
     * Attaches this interaction to another entity, making it ride the target entity.
     * <b>Warning:</b> Keep in mind that the interaction's location is not automatically
     * updated when it is attached to another entity, so if the entity moves too far
     * away from the interaction's location, the interaction may be unloaded.
     * To work around this, you will need to teleport the interaction to the entity's
     * location when the entity is moved far away from the interaction's original location.
     *
     * @param entityId The entity id to attach the interaction to
     */
    @ApiStatus.Experimental
    public void attach(int entityId) {
        attachedEntityId = entityId;

        if (renderMode == RenderMode.NOT_ATTACHED_PLAYER) {
            Player attachedPlayer = getPlayerByEntityId(entityId);
            if (attachedPlayer != null) {
                removeViewer(attachedPlayer);
            }
        }

        WrapperPlayServerSetPassengers attachPacket = new WrapperPlayServerSetPassengers(entityId, addElement(PassengerManager.getPassengers(entityId), this.entityID));
        BukkitTasks.runTaskAsync(() -> this.entity.sendPacketsToViewers(attachPacket));
    }

    /**
     * Detaches the interaction from its attached entity, if any.
     */
    @ApiStatus.Experimental
    public void detach() {
        if (attachedEntityId != null) {
            if (renderMode == RenderMode.NOT_ATTACHED_PLAYER) {
                Player previouslyAttachedPlayer = getPlayerByEntityId(attachedEntityId);
                if (previouslyAttachedPlayer != null) {
                    addViewer(previouslyAttachedPlayer);
                }
            }

            WrapperPlayServerSetPassengers detachPacket = new WrapperPlayServerSetPassengers(attachedEntityId, removeElement(PassengerManager.getPassengers(attachedEntityId), this.entityID));
            BukkitTasks.runTaskAsync(() -> this.entity.sendPacketsToViewers(detachPacket));
            attachedEntityId = null;
        }
    }

    /**
     * Adds an element to the array if it is not already present.
     */
    public static int[] addElement(int[] array, int element) {
        for (int value : array) if (value == element) return array;

        int[] result = Arrays.copyOf(array, array.length + 1);
        result[array.length] = element;
        return result;
    }

    /**
     * Removes all occurrences of the specified element from the given array.
     */
    public static int[] removeElement(int[] array, int element) {
        int count = 0;
        for (int value : array) if (value == element) count++;

        if (count == 0) return array;
        int[] result = new int[array.length - count];
        int i = 0;
        for (int value : array) if (value != element) result[i++] = value;
        return result;
    }

    /**
     * Attaches entities to this interaction.
     * @param entityIDs The passengers
     */
    public void addPassenger(int... entityIDs) {
        this.entity.addPassengers(entityIDs);
    }

    public Set<Integer> getPassengers() {
        return this.entity.getPassengers();
    }

    public InteractionBox setUpdateTaskPeriod(long updateTaskPeriod) {
        this.updateTaskPeriod = updateTaskPeriod;
        return this;
    }

    public InteractionBox setMaxPlayerRenderDistanceSquared(double maxPlayerRenderDistanceSquared) {
        this.maxPlayerRenderDistanceSquared = maxPlayerRenderDistanceSquared;
        return this;
    }

    public InteractionBox setWidth(float width) {
        this.width = width;
        return this;
    }

    public InteractionBox setHeight(float height) {
        this.height = height;
        return this;
    }

    public InteractionBox setSize(Vector3F size) {
        this.width = size.getX();
        this.height = size.getY();
        return this;
    }

    public InteractionBox setSize(float width, float height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public InteractionBox setResponsive(boolean responsive) {
        this.responsive = responsive;
        return this;
    }

    public InteractionBox addViewer(Player player) {
        this.entity.addViewer(player.getUniqueId());
        if (attachedEntityId != null)
            sendPacket(new WrapperPlayServerSetPassengers(
                            attachedEntityId, addElement(PassengerManager.getPassengers(attachedEntityId), this.entityID)),
                    Collections.singletonList(player)
            );
        return this;
    }

    public InteractionBox removeViewer(Player player) {
        this.entity.removeViewer(player.getUniqueId());
        return this;
    }

    public Set<UUID> getViewerUUIDs() {
        return this.entity.getViewers();
    }

    public List<Player> getViewers() {
        Set<UUID> viewerUUIDs = this.entity.getViewers();
        List<Player> viewers = new ArrayList<>(viewerUUIDs.size());
        viewerUUIDs.forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                viewers.add(player);
            }
        });
        return viewers;
    }

    public InteractionBox addAllViewers(List<Player> viewerList) {
        viewerList.forEach(this::addViewer);
        return this;
    }

    public InteractionBox removeAllViewers() {
        this.entity.getViewers().forEach(this.entity::removeViewer);
        return this;
    }

    public void triggerInteraction(Player player) {
        this.onInteract.onInteract(player);
    }

    /**
     * Creates a copy of this interaction with a new ID.
     * The new ID will be the original ID with '_copy_' followed by a random number appended.
     * @return A new HologramInteraction instance with copied properties
     */
    public InteractionBox copy() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000);
        return this.copy(this.id + "_copy_" + randomNumber);
    }

    /**
     * Creates a copy of this interaction with a new ID.
     * @return A new HologramInteraction instance with copied properties
     */
    public InteractionBox copy(String id) {
        InteractionBox copy = new InteractionBox(id, this.renderMode, this.onInteract);
        copy.width = this.width;
        copy.height = this.height;
        copy.responsive = this.responsive;
        copy.updateTaskPeriod = this.updateTaskPeriod;
        copy.maxPlayerRenderDistanceSquared = this.maxPlayerRenderDistanceSquared;
        return copy;
    }

    /**
     * Gets the size as a Vector3F (width, height, 0).
     */
    public Vector3F getSize() {
        return new Vector3F(this.width, this.height, 0);
    }
}
