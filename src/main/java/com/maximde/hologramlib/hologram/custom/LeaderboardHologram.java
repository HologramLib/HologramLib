package com.maximde.hologramlib.hologram.custom;

import com.maximde.hologramlib.hologram.ItemHologram;
import com.maximde.hologramlib.hologram.RenderMode;
import com.maximde.hologramlib.hologram.TextHologram;
import com.maximde.hologramlib.utils.PlayerHeadComponent;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.ApiStatus;

import java.text.NumberFormat;
import java.util.*;

@Getter
@ApiStatus.Experimental
public class LeaderboardHologram {

    private static final Map<String, LeaderboardHologram> hologramIdToLeaderboard = new WeakHashMap<>();

    private final String baseId;
    private final TextHologram textHologram;
    /**
     *
     * @return The first place head ItemHologram, or null
     */
    private ItemHologram firstPlaceHead;

    private Map<UUID, PlayerScore> playerData = new HashMap<>();
    private Location baseLocation;
    /**
     * @return The yaw rotation
     */
    private float xRotation = 0;

    @Setter
    @Accessors(chain = true)
    private LeaderboardOptions options;

    /**
     * Creates a leaderboard with the specified options and ID.
     *
     * @param options Configuration options for the leaderboard
     * @param id Unique identifier (cannot contain spaces)
     */
    public LeaderboardHologram(LeaderboardOptions options, String id) {
        validateId(id);
        this.baseId = id;
        this.options = options;

        this.textHologram = new TextHologram(baseId + "_text", RenderMode.ALL);
        configureTextHologram();

        hologramIdToLeaderboard.put(textHologram.getId(), this);

        if (options.leaderboardType() == LeaderboardType.TOP_PLAYER_HEAD) {
            this.firstPlaceHead = new ItemHologram(baseId + "_head", RenderMode.ALL);
        }
    }

    /**
     * Gets the LeaderboardHologram associated with a specific text hologram ID.
     * Used internally for bedrock player filtering.
     *
     * @param textHologramId The ID of the text hologram
     * @return The associated LeaderboardHologram, or null if not found
     */
    public static LeaderboardHologram getLeaderboardByTextHologramId(String textHologramId) {
        return hologramIdToLeaderboard.get(textHologramId);
    }

    protected void validateId(String id) {
        if (id.contains(" ")) {
            throw new IllegalArgumentException("The hologram ID cannot contain spaces! (" + id + ")");
        }
    }

    private void configureTextHologram() {
        textHologram
                .setAlignment(options.dottedLayoutEnabled() ? TextDisplay.TextAlignment.LEFT : TextDisplay.TextAlignment.CENTER)
                .setBillboard(options.rotationMode() == RotationMode.DYNAMIC
                        ? Display.Billboard.VERTICAL
                        : Display.Billboard.FIXED)
                .setBackgroundColor(options.background() ? options.backgroundColor() : 0)
                .setSeeThroughBlocks(false);
    }

    /**
     * Sets or updates the score for a player.
     *
     * @param uuid Player's UUID
     * @param name Player's display name
     * @param score Player's score (double)
     */
    public void setPlayerScore(UUID uuid, String name, double score) {
        playerData.put(uuid, new PlayerScore(name, score));
    }

    /**
     * Sets or updates the score for a player.
     *
     * @param uuid Player's UUID
     * @param name Player's display name
     * @param score Player's score (long)
     */
    public void setPlayerScore(UUID uuid, String name, long score) {
        playerData.put(uuid, new PlayerScore(name, score));
    }

    /**
     * Replaces all scores with the provided data.
     *
     * @param data Map of UUID to PlayerScore
     */
    public void setAllScores(Map<UUID, PlayerScore> data) {
        this.playerData = new HashMap<>(data);
    }

    /**
     * Removes a player from the leaderboard.
     *
     * @param uuid Player's UUID to remove
     */
    public void removePlayer(UUID uuid) {
        playerData.remove(uuid);
    }


    private String buildLeaderboardText() {
        if (playerData.isEmpty()) {
            return "";
        }

        List<Map.Entry<UUID, PlayerScore>> sorted = getSortedEntries();
        StringBuilder text = new StringBuilder();

        text.append(options.titleFormat().replace("{title}", options.title()));

        if (options.leaderboardType() == LeaderboardType.TOP_PLAYER_HEAD && !sorted.isEmpty()) {
            text.append("\n\n\n\n\n\n");
        } else {
            text.append("\n\n");
        }

        int maxEntries = options.maxDisplayEntries();
        int displayCount = Math.min(sorted.size(), maxEntries);

        int maxLineLength = 0;
        List<FormattedEntry> formattedEntries = new ArrayList<>();

        if (options.dottedLayoutEnabled()) {
            for (int i = 0; i < displayCount; i++) {
                Map.Entry<UUID, PlayerScore> entry = sorted.get(i);
                UUID uuid = entry.getKey();
                PlayerScore playerScore = entry.getValue();
                int place = i + 1;

                FormattedEntry formatted = formatEntryForDottedLayout(place, uuid, playerScore);
                formattedEntries.add(formatted);
                maxLineLength = Math.max(maxLineLength, formatted.baseLength);
            }

            maxLineLength += options.dottedLayoutAdditionalDots();
        }

        for (int i = 0; i < displayCount; i++) {
            String content;

            if (options.dottedLayoutEnabled()) {
                FormattedEntry formatted = formattedEntries.get(i);
                int dotsNeeded = maxLineLength - formatted.baseLength;
                content = formatted.formatWithDots(dotsNeeded);
            } else {
                Map.Entry<UUID, PlayerScore> entry = sorted.get(i);
                UUID uuid = entry.getKey();
                PlayerScore playerScore = entry.getValue();
                int place = i + 1;
                content = getFormattedEntry(place, uuid, playerScore);
            }

            text.append(content);

            if (i < displayCount - 1) {
                text.append("\n");
            }
        }

        if (options.showEmptyPlaces()) {
            for (int i = displayCount; i < maxEntries; i++) {
                text.append("\n").append(getEmptyPlaceFormat(i + 1));
            }
        }

        text.append("\n\n").append(options.footerFormat());

        return text.toString();
    }

    private String getFormattedEntry(int place, UUID uuid, PlayerScore playerScore) {
        String placeFormat = (place <= 3 && place <= options.placeFormats().length)
                ? options.placeFormats()[place - 1]
                : options.defaultPlaceFormat();

        String headTag = "";
        if (options.leaderboardType() == LeaderboardType.ALL_PLAYER_HEADS) {
            headTag = PlayerHeadComponent.fromPlayer(playerScore.name());
        }

        return placeFormat
                .replace("{place}", String.valueOf(place))
                .replace("{name}", playerScore.name())
                .replace("{score}", formatScore(playerScore.score()))
                .replace("{suffix}", options.suffix())
                .replace("{extra}", options.extra().getOrDefault(uuid, ""))
                .replace("{head}", headTag);
    }

    private String getEmptyPlaceFormat(int place) {
        return options.defaultPlaceFormat()
                .replace("{place}", String.valueOf(place))
                .replace("{name}", "-------")
                .replace("{score}", "-- --")
                .replace("{suffix}", options.suffix())
                .replace("{extra}", "")
                .replace("{head}", "");
    }

    /**
     * Helper class to store formatted entry components for dotted layout
     */
    private static class FormattedEntry {
        String placeAndName;
        String extra;
        String score;
        int baseLength;

        FormattedEntry(String placeAndName, String extra, String score, int baseLength) {
            this.placeAndName = placeAndName;
            this.extra = extra;
            this.score = score;
            this.baseLength = baseLength;
        }

        String formatWithDots(int dotsCount) {
            String dots = ".".repeat(Math.max(0, dotsCount));
            return placeAndName + extra + " " + dots + score;
        }
    }

    /**
     * Formats an entry for dotted layout mode.
     * Returns components that can be assembled with dots later.
     */
    private FormattedEntry formatEntryForDottedLayout(int place, UUID uuid, PlayerScore playerScore) {
        String placeFormat = (place <= 3 && place <= options.placeFormats().length)
                ? options.placeFormats()[place - 1]
                : options.defaultPlaceFormat();

        String headTag = "";
        if (options.leaderboardType() == LeaderboardType.ALL_PLAYER_HEADS) {
            headTag = PlayerHeadComponent.fromPlayer(playerScore.name());
        }

        String placeAndName = placeFormat
                .replace("{place}", String.valueOf(place))
                .replace("{name}", playerScore.name())
                .replace("{score}", "")
                .replace("{suffix}", "")
                .replace("{extra}", "")
                .replace("{head}", headTag);

        String extra = options.extra().getOrDefault(uuid, "");
        if (!extra.isEmpty()) {
            extra = " " + extra;
        }

        String formattedScore = formatScore(playerScore.score());
        String suffix = options.suffix().isEmpty() ? "" : " " + options.suffix();
        String score = formattedScore + suffix;

        int visualLength = stripMiniMessageTags(placeAndName).length()
                + stripMiniMessageTags(extra).length()
                + 1
                + stripMiniMessageTags(score).length();

        return new FormattedEntry(placeAndName, extra, score, visualLength);
    }

    /**
     * Strips MiniMessage formatting tags to calculate visual length.
     * This is a simplified version - for more accurate results, you might want to use MiniMessage's parser.
     */
    private String stripMiniMessageTags(String text) {
        return text.replaceAll("<[^>]+>", "");
    }

    private void updateFirstPlaceHead() {
        if (options.leaderboardType() != LeaderboardType.TOP_PLAYER_HEAD) {
            return;
        }

        if (playerData.isEmpty() || firstPlaceHead == null) {
            return;
        }

        List<Map.Entry<UUID, PlayerScore>> sorted = getSortedEntries();
        if (sorted.isEmpty()) {
            return;
        }

        UUID firstUuid = sorted.get(0).getKey();

        float headY = (float) (sorted.size() * options.lineHeight()) + 1.1f;

        firstPlaceHead
                .setPlayerHead(firstUuid)
                .setScale(2.0f, 2.0f, 0.01f)
                .setTranslation(0f, headY, 0f)
                .setBillboard(options.rotationMode() == RotationMode.DYNAMIC
                        ? Display.Billboard.VERTICAL
                        : Display.Billboard.FIXED);

        if (!firstPlaceHead.isDead() && baseLocation != null) {
            firstPlaceHead.teleport(baseLocation);
            firstPlaceHead.update();
        }
    }

    /**
     * Updates the leaderboard display with current player data.
     * Call this after modifying scores to refresh the visual display.
     */
    public void update() {
        if (baseLocation == null) {
            return;
        }

        String leaderboardText = buildLeaderboardText();
        textHologram.setMiniMessageText(leaderboardText);

        if (!textHologram.isDead()) {
            textHologram.update();
        }

        if (options.leaderboardType() == LeaderboardType.TOP_PLAYER_HEAD) {
            updateFirstPlaceHead();
        }
    }

    /**
     * Spawns the leaderboard at the specified location.
     * This is the initial spawn - creates the hologram entities.
     *
     * @param location The spawn location
     * @return This leaderboard for chaining
     */
    public LeaderboardHologram spawn(Location location) {
        return spawn(location, false);
    }

    /**
     * Spawns the leaderboard at the specified location.
     *
     * @param location The spawn location
     * @param ignorePitchYaw If true, ignores the pitch and yaw of the location
     * @return This leaderboard for chaining
     */
    public LeaderboardHologram spawn(Location location, boolean ignorePitchYaw) {
        this.baseLocation = location.clone();

        textHologram.getInternalAccess().spawn(location, ignorePitchYaw);

        update();

        if (options.leaderboardType() == LeaderboardType.TOP_PLAYER_HEAD
                && firstPlaceHead != null
                && firstPlaceHead.isDead()) {
            firstPlaceHead.getInternalAccess().spawn(location, ignorePitchYaw);
            firstPlaceHead.update();
        }

        return this;
    }

    /**
     * Teleports the leaderboard to a new location.
     * If not yet spawned, this will spawn it first.
     * Automatically calls {@link #update()} to refresh the display.
     *
     * @param location The new location
     * @return This leaderboard for chaining
     */
    public LeaderboardHologram teleport(Location location) {
        if (textHologram.isDead()) {
            return spawn(location);
        }

        this.baseLocation = location.clone();

        textHologram.teleport(baseLocation);

        if (firstPlaceHead != null && !firstPlaceHead.isDead()) {
            firstPlaceHead.teleport(baseLocation);
        }

        update();
        return this;
    }

    /**
     * Shows the leaderboard to a specific player.
     *
     * @param player The player to show the leaderboard to
     */
    public void show(Player player) {
        textHologram.show(player);

        if (firstPlaceHead != null) {
            firstPlaceHead.show(player);
        }
    }

    /**
     * Hides the leaderboard from a specific player.
     *
     * @param player The player to hide the leaderboard from
     */
    public void hide(Player player) {
        textHologram.hide(player);

        if (firstPlaceHead != null) {
            firstPlaceHead.hide(player);
        }
    }


    /**
     * Rotates the leaderboard.
     *
     * @param x Yaw rotation
     * @param y Pitch rotation
     * @return This leaderboard for chaining
     */
    @ApiStatus.Experimental
    public LeaderboardHologram rotate(float x, float y) {
        this.xRotation = x;

        textHologram.setRotation(x, y);
        if (!textHologram.isDead()) {
            textHologram.update();
        }

        if (firstPlaceHead != null) {
            firstPlaceHead.setRotation(x, y);
            if (!firstPlaceHead.isDead()) {
                firstPlaceHead.update();
            }
        }

        return this;
    }

    /**
     * Sets the leaderboard to fixed rotation mode.
     * The leaderboard will not rotate to face players.
     *
     * @return This leaderboard for chaining
     */
    public LeaderboardHologram setFixedRotation() {
        options.rotationMode(RotationMode.FIXED);

        textHologram.setBillboard(Display.Billboard.FIXED);

        if (firstPlaceHead != null) {
            firstPlaceHead.setBillboard(Display.Billboard.FIXED);
        }

        return this;
    }


    private List<Map.Entry<UUID, PlayerScore>> getSortedEntries() {
        List<Map.Entry<UUID, PlayerScore>> sorted = new ArrayList<>(playerData.entrySet());

        Comparator<Map.Entry<UUID, PlayerScore>> comparator =
                Comparator.comparingDouble(e -> e.getValue().score().doubleValue());

        if (options.sortOrder() == SortOrder.DESCENDING) {
            comparator = comparator.reversed();
        }

        sorted.sort(comparator);
        return sorted.subList(0, Math.min(sorted.size(), options.maxDisplayEntries()));
    }

    private String formatScore(Number n) {
        NumberFormat nf = options.numberFormat();
        if (nf != null) {
            return nf.format(n);
        }

        NumberFormat def;
        if (options.decimalNumbers()) {
            def = NumberFormat.getNumberInstance(options.numberLocale());
            def.setGroupingUsed(true);
            def.setMinimumFractionDigits(0);
            def.setMaximumFractionDigits(Math.max(0, options.maxFractionDigits()));
            return def.format(n.doubleValue());
        } else {
            def = NumberFormat.getIntegerInstance(options.numberLocale());
            def.setGroupingUsed(true);
            return def.format(n.longValue());
        }
    }


    /**
     * Gets the leaderboard's current location.
     *
     * @return A clone of the base location, or null if not yet positioned
     */
    public Location getLocation() {
        return baseLocation != null ? baseLocation.clone() : null;
    }

    /**
     * Checks if the leaderboard is dead (not spawned or has been killed).
     *
     * @return true if the leaderboard is dead
     */
    public boolean isDead() {
        return textHologram.isDead();
    }

    /**
     * Kills the leaderboard and all its entities.
     *
     * @deprecated Use HologramManager.remove() instead for proper cleanup
     */
    @Deprecated
    public void kill() {
        hologramIdToLeaderboard.remove(textHologram.getId());

        if (firstPlaceHead != null && !firstPlaceHead.isDead()) {
            firstPlaceHead.getInternalAccess().kill();
        }

        if (!textHologram.isDead()) {
            textHologram.getInternalAccess().kill();
        }
    }

    /**
     * Gets all text holograms used by this leaderboard.
     * In the refactored version, this returns a single-element list.
     *
     * @return List containing the main text hologram
     */
    public List<TextHologram> getAllTextHolograms() {
        return Collections.singletonList(textHologram);
    }

    /**
     * Gets individual entry holograms.
     *
     * @deprecated The refactored version uses a single text hologram.
     *             This method returns an empty list for backward compatibility.
     * @return Empty list
     */
    @Deprecated
    public List<TextHologram> getEntryHolograms() {
        return new ArrayList<>();
    }


    /**
     * Functional interface for detecting bedrock players.
     * Used to determine if special rendering adjustments should be applied.
     */
    @FunctionalInterface
    public interface BedrockPlayerDetector {
        /**
         * Checks if a player is a bedrock player.
         *
         * @param player The player to check
         * @return true if the player is a bedrock player, false otherwise
         */
        boolean isBedrockPlayer(Player player);
    }

    /**
     * Represents a player's score entry.
     *
     * @param name Player's display name
     * @param score Player's score (can be Long or Double)
     */
    public record PlayerScore(String name, Number score) {}

    public enum LeaderboardType {
        /** Simple text leaderboard without player heads */
        SIMPLE_TEXT,

        /** Shows first place player's head above the leaderboard */
        TOP_PLAYER_HEAD,

        /** Shows all player heads inline with their names */
        @ApiStatus.Experimental
        ALL_PLAYER_HEADS
    }

    public enum RotationMode {
        /** Billboard rotates to always face the player */
        DYNAMIC,

        /** Billboard has fixed rotation */
        FIXED
    }

    /**
     * @deprecated Head mode is no longer configurable. All heads use inline tags.
     */
    @Deprecated
    public enum HeadMode {
        @Deprecated RESOURCEPACK,
        @Deprecated ITEM_DISPLAY
    }

    public enum SortOrder {
        /** Highest scores first */
        DESCENDING,

        /** Lowest scores first */
        ASCENDING
    }

    /**
     * Configuration options for customizing the leaderboard display.
     * Use the builder pattern to create instances.
     */
    @Data
    @Builder
    @Accessors(fluent = true)
    public static class LeaderboardOptions {

        @Builder.Default
        private float backgroundWidth = 40f;

        @Builder.Default
        private String title = "Leaderboard";

        /**
         * Format strings for top 3 places.
         * Supports: {place}, {name}, {score}, {suffix}, {extra}, {head}
         */
        @Builder.Default
        private String[] placeFormats = new String[]{
                "<color:#fdcc00><bold>1.</bold></color> {head} <color:#fdcc00>{name}</color> {extra} <gray>{score}</gray> <white>{suffix}</white>",
                "<color:#dcdcdc><bold>2.</bold></color> {head} <color:#dcdcdc>{name}</color> {extra} <gray>{score}</gray> <white>{suffix}</white>",
                "<color:#e65f2f><bold>3.</bold></color> {head} <color:#e65f2f>{name}</color> {extra} <gray>{score}</gray> <white>{suffix}</white>"
        };

        /**
         * Default format for places 4+.
         * Supports: {place}, {name}, {score}, {suffix}, {extra}, {head}
         */
        @Builder.Default
        private String defaultPlaceFormat = "<color:#ffb486><bold>{place}.</bold></color> {head} <color:#ffb486>{name}</color> {extra} <gray>{score}</gray> <white>{suffix}</white>";

        @Builder.Default
        private String titleFormat = "<gradient:#ff6000:#ffc663>--------- {title} ---------</gradient>";

        @Builder.Default
        private String footerFormat = "<gradient:#ffc663:#ff6000>----------------------------</gradient>";

        /** Suffix to append after scores (e.g., "Kills", "Points") */
        @Builder.Default
        private String suffix = "";

        @Builder.Default
        private LeaderboardType leaderboardType = LeaderboardType.TOP_PLAYER_HEAD;

        /** Show empty placeholder entries when fewer players than maxDisplayEntries */
        @Builder.Default
        private boolean showEmptyPlaces = false;

        /** Maximum number of entries to display */
        @Builder.Default
        private int maxDisplayEntries = 10;

        @Builder.Default
        private SortOrder sortOrder = SortOrder.DESCENDING;

        @Builder.Default
        private RotationMode rotationMode = RotationMode.DYNAMIC;

        /**
         * @deprecated No longer used. All heads use inline component tags.
         */
        @Deprecated
        @Builder.Default
        private HeadMode headMode = HeadMode.ITEM_DISPLAY;

        /** Extra text per player (e.g., emojis, badges). Maps UUID to string. */
        @Builder.Default
        private Map<UUID, String> extra = new HashMap<>();

        /** Vertical spacing between entries (in blocks) */
        @Builder.Default
        private double lineHeight = 0.25;

        @Builder.Default
        private boolean background = true;

        /** If true, formats scores as decimals. If false, as integers */
        @Builder.Default
        private boolean decimalNumbers = false;

        /** Custom number formatter. If null, uses default based on decimalNumbers */
        @Builder.Default
        private NumberFormat numberFormat = null;

        @Builder.Default
        private Locale numberLocale = Locale.GERMANY;

        /** Maximum decimal places when decimalNumbers is true */
        @Builder.Default
        private int maxFractionDigits = 2;

        @Builder.Default
        private int backgroundColor = 0x54000000;

        /** Enables bedrock-specific rendering fixes (like, hiding player heads for bedrock clients) */
        @Builder.Default
        private boolean bedrockSupportEnabled = true;

        /**
         * Custom detector for identifying bedrock players.
         * Default implementation checks if player name starts with "."
         */
        @Builder.Default
        private BedrockPlayerDetector bedrockPlayerDetector = player ->
                player != null && player.getName() != null && player.getName().startsWith(".");

        /**
         * Enables a dotted layout where each line has consistent total width.
         * When enabled, dots are dynamically inserted between player name/extra and score
         * to ensure all lines are the same length. Text alignment is automatically set to LEFT.
         */
        @Builder.Default @ApiStatus.Experimental
        private boolean dottedLayoutEnabled = false;

        /**
         * Number of additional dots to add to all lines when dottedLayoutEnabled is true
         * If set to 0, lines will be padded to match the longest line
         * If set to a positive number, that many extra dots will be added to ALL lines
         * (including the longest one).
         */
        @Builder.Default @ApiStatus.Experimental
        private int dottedLayoutAdditionalDots = 10;
    }
}