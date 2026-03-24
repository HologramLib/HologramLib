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
import com.maximde.hologramlib.hologram.custom.LeaderboardHologram;
import com.maximde.hologramlib.utils.MiniMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Packet listener that filters out player head components from text displays
 * for bedrock players to prevent rendering issues.
 */
public class BedrockPlayerHeadFilter implements PacketListener {

    private static final int TEXT_DISPLAY_TEXT_INDEX = 23;

    private static final Pattern HEAD_TAG_PATTERN = Pattern.compile("<head:[^>]+>");

    public BedrockPlayerHeadFilter(PacketEventsAPI<?> packetEventsAPI) {
        packetEventsAPI.getEventManager().registerListener(this, PacketListenerPriority.LOW);
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_METADATA) return;

        Player player = event.getPlayer();

        handleMetadataPacket(event, player);
    }

    private void handleMetadataPacket(PacketSendEvent event, Player player) {
        WrapperPlayServerEntityMetadata packet = new WrapperPlayServerEntityMetadata(event);
        int entityId = packet.getEntityId();

        Optional<com.maximde.hologramlib.hologram.HologramManager> managerOpt = HologramLib.getManager();
        if (managerOpt.isEmpty()) return;

        Optional<Hologram<?>> hologramOptional = managerOpt.get().getHologramByEntityId(entityId);
        if (hologramOptional.isEmpty()) return;

        Hologram<?> hologram = hologramOptional.get();
        if (!(hologram instanceof TextHologram textHologram)) return;

        LeaderboardHologram leaderboard = findAssociatedLeaderboard(textHologram);
        if (leaderboard == null) return;

        LeaderboardHologram.LeaderboardOptions options = leaderboard.getOptions();

        if (!options.bedrockSupportEnabled()) return;

        LeaderboardHologram.BedrockPlayerDetector detector = options.bedrockPlayerDetector();
        if (detector == null || !detector.isBedrockPlayer(player)) return;

        List<EntityData<?>> newMetadata = new ArrayList<>();
        boolean textModified = false;

        for (EntityData<?> data : packet.getEntityMetadata()) {
            if (data.getIndex() == TEXT_DISPLAY_TEXT_INDEX &&
                    data.getType() == EntityDataTypes.ADV_COMPONENT) {

                Component originalComponent = (Component) data.getValue();
                String serialized = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().serialize(originalComponent);

                String filteredText = HEAD_TAG_PATTERN.matcher(serialized).replaceAll("");

                if (!serialized.equals(filteredText)) {
                    Component filteredComponent = MiniMessage.get(filteredText);
                    newMetadata.add(new EntityData<>(
                            TEXT_DISPLAY_TEXT_INDEX,
                            EntityDataTypes.ADV_COMPONENT,
                            filteredComponent
                    ));
                    textModified = true;
                } else {
                    newMetadata.add(data);
                }
            } else {
                newMetadata.add(data);
            }
        }

        if (textModified) {
            packet.setEntityMetadata(newMetadata);
        }
    }

    private LeaderboardHologram findAssociatedLeaderboard(TextHologram textHologram) {
        return LeaderboardHologram.getLeaderboardByTextHologramId(textHologram.getId());
    }
}