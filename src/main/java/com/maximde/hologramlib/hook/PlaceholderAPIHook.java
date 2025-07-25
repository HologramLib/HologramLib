package com.maximde.hologramlib.hook;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.entity.data.EntityData;
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import com.maximde.hologramlib.HologramLib;
import com.maximde.hologramlib.hologram.Hologram;
import com.maximde.hologramlib.hologram.TextHologram;
import com.maximde.hologramlib.utils.MiniMessage;
import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PlaceholderAPIHook implements PacketListener {

    private static final int TEXT_DISPLAY_TEXT_INDEX = 23; // The index for the message in the Entity Metadata packet (It worked on my side, tbh i dont feel like this is safe, but it works for now)

    public PlaceholderAPIHook(PacketEventsAPI<?> packetEventsAPI) {
        packetEventsAPI.getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        int entityId = packet.getEntityId();

        Optional<com.maximde.hologramlib.hologram.HologramManager> managerOpt = HologramLib.getManager();
        if (managerOpt.isEmpty()) {
            return;
        }

        Optional<Hologram<?>> hologramOptional = managerOpt.get().getHologramByEntityId(entityId);
        if (hologramOptional.isEmpty()) {
            return;
        }

        Hologram<?> hologram = hologramOptional.get();
        if (!(hologram instanceof TextHologram textHologram)) {
            return;
        }

        if (!textHologram.isPlaceholderApiEnabled()) {
            return;
        }

        Player player = (Player) event.getPlayer();
        if (player == null) {
            return;
        }

        String rawText = textHologram.getRawText();
        if (rawText == null || rawText.isEmpty()) {
            return;
        }

        String parsedText = PlaceholderAPI.setPlaceholders(player, rawText);
        Component finalComponent = MiniMessage.get(textHologram.replaceFontImages(parsedText));

        List<EntityData<?>> newMetadata = new ArrayList<>();
        boolean textMetaFound = false;

        for (EntityData<?> data : packet.getEntityMetadata()) {
            if (data.getIndex() == TEXT_DISPLAY_TEXT_INDEX &&
                data.getType() == EntityDataTypes.ADV_COMPONENT) {

                newMetadata.add(new EntityData<>(
                    TEXT_DISPLAY_TEXT_INDEX,
                    EntityDataTypes.ADV_COMPONENT,
                    finalComponent
                ));
                textMetaFound = true;
            } else {
                newMetadata.add(data);
            }
        }

        if (!textMetaFound) {
            newMetadata.add(new EntityData<>(
                TEXT_DISPLAY_TEXT_INDEX,
                EntityDataTypes.ADV_COMPONENT,
                finalComponent
            ));
        }

        packet.setEntityMetadata(newMetadata);
    }
}