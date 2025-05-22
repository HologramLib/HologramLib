package com.maximde.hologramlib;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class Main extends JavaPlugin {

    @Override
    public void onLoad() {
        HologramLib.onLoad(this);
    }

    @Override
    public void onEnable() {
        HologramLib.init();
    }

    @Override
    public void onDisable() {
        HologramLib.onDisable();
        Optional.ofNullable(PacketEvents.getAPI()).ifPresent(PacketEventsAPI::terminate);
    }
}
