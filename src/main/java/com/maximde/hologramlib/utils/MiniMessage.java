package com.maximde.hologramlib.utils;

import net.kyori.adventure.text.Component;

public class MiniMessage {
    private static final net.kyori.adventure.text.minimessage.MiniMessage minimessage = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();
    public static Component get(String message) {
        return minimessage.deserialize(message);
    }

    public static String stripTags(String message) {
        return minimessage.stripTags(message);
    }
}
