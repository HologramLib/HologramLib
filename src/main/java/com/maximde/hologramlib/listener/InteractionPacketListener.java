package com.maximde.hologramlib.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.maximde.hologramlib.hologram.HologramManager;
import com.maximde.hologramlib.hologram.InteractionBox;
import com.maximde.hologramlib.utils.BukkitTasks;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class InteractionPacketListener implements PacketListener {

    private final HologramManager hologramManager;

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.INTERACT_ENTITY) return;

        WrapperPlayClientInteractEntity packet = new WrapperPlayClientInteractEntity(event);
        int entityId = packet.getEntityId();
        Optional<InteractionBox> interactionBox = hologramManager.getInteractionBoxByEntityId(entityId);

        if (interactionBox.isEmpty()) return;
        WrapperPlayClientInteractEntity.InteractAction interactAction = packet.getAction();

        UUID playerUUID = event.getUser().getUUID();
        BukkitTasks.runTask(() -> {
            Player player = Bukkit.getPlayer(playerUUID);

            if (player != null && player.isOnline()) {
                org.bukkit.Location boxLocation = interactionBox.get().getLocation();

                if (boxLocation == null || boxLocation.getWorld() == null) return;

                if (!player.getWorld().equals(boxLocation.getWorld())) return;

                double maxDistanceSquared = 6.0 * 6.0;
                if (player.getLocation().distanceSquared(boxLocation) > maxDistanceSquared) return;

                interactionBox.get().triggerInteraction((player));
            }
        });
    }
}