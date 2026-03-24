package com.maximde.hologramlib.utils;


public class PlayerHeadComponent {

    public static String fromPlayer(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return "";
        }
        return "<head:" + playerName + ">";
    }
}
