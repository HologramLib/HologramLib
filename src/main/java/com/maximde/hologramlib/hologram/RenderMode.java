package com.maximde.hologramlib.hologram;

/**
 * Defines how a hologram should be rendered and to which players.
 */
public enum RenderMode {
    /**
     * Hologram is not shown to any players
     */
    NONE,
    /**
     * Hologram is only rendered to players in its viewer list (where players have to be added manually)
     */
    VIEWER_LIST,
    /**
     * Hologram is rendered to all players on the server
     */
    ALL,
    /**
     * Use RenderMode.ALL instead! It does the exact same thing.
     */
    @Deprecated
    NEARBY,

    /**
     * Works like render mode ALL with the difference if the hologram is attached to a player
     * the player will not see the hologram
     */
    NOT_ATTACHED_PLAYER
}