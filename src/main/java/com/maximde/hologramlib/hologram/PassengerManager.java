package com.maximde.hologramlib.hologram;

import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPassengers;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PassengerManager implements PacketListener {

    private static final Map<Integer, int[]> passengers = new HashMap<>();

    public PassengerManager(PacketEventsAPI<?> packetEventsAPI) {
        packetEventsAPI.getEventManager().registerListener(this, PacketListenerPriority.NORMAL);
    }

    @Override
    public void onPacketSend(@NotNull PacketSendEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.SET_PASSENGERS) return;
        WrapperPlayServerSetPassengers packet = new WrapperPlayServerSetPassengers(event);
        passengers.put(packet.getEntityId(), packet.getPassengers());
    }

    public static int[] getPassengers(int entityId) {
        return passengers.getOrDefault(entityId, new int[0]);
    }
}
