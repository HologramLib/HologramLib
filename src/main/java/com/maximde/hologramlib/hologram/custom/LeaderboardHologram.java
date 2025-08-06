package com.maximde.hologramlib.hologram.custom;

import com.maximde.hologramlib.hologram.ItemHologram;
import com.maximde.hologramlib.hologram.TextHologram;
import com.maximde.hologramlib.utils.PlayerUtils;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.List;
import java.util.logging.Level;

@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class LeaderboardHologram {

    private final String baseId;
    private final TextHologram headerHologram;
    private final TextHologram footerHologram;
    private final ItemHologram firstPlaceHead;
    private final List<TextHologram> entryHolograms;
    private final List<TextHologram> allTextHolograms;
    private final TextHologram backgroundHologram;

    private int leaderboardEntries = 0;
    private Map<UUID, PlayerScore> playerData = new HashMap<>();
    private Location baseLocation;

    public enum LeaderboardType {
        SIMPLE_TEXT,
        TOP_PLAYER_HEAD,
        @ApiStatus.Experimental
        ALL_PLAYER_HEADS
    }

    public enum RotationMode {
        DYNAMIC,
        FIXED
    }

    public enum HeadMode {
        @ApiStatus.Experimental
        RESOURCEPACK,
        ITEM_DISPLAY
    }

    public enum SortOrder {
        DESCENDING,
        ASCENDING,
    }

    private final int scale = 1;

    @Data
    @Builder
    @Accessors(fluent = true)
    public static class LeaderboardOptions {

        @Builder.Default
        public float backgroundWidth = 40f;

        @Builder.Default
        private String title = "Leaderboard";

        @Builder.Default
        private String[] placeFormats = new String[] {
                "<color:#fdcc00><bold>1.</bold></color> {head} <color:#fdcc00>{name}</color> {extra} <gray>{score}</gray> <white>{suffix}</white>",
                "<color:#dcdcdc><bold>2.</bold></color> {head} <color:#dcdcdc>{name}</color> {extra} <gray>{score}</gray> <white>{suffix}</white>",
                "<color:#e65f2f><bold>3.</bold></color> {head} <color:#e65f2f>{name}</color> {extra} <gray>{score}</gray> <white>{suffix}</white>"
        };

        @Builder.Default
        private String defaultPlaceFormat = "<color:#ffb486><bold>{place}.</bold></color> {head} <color:#ffb486>{name}</color> {extra} <gray>{score}</gray> <white>{suffix}</white>";

        @Builder.Default
        private String titleFormat = "<gradient:#ff6000:#ffc663>--------- {title} ---------</gradient>";

        @Builder.Default
        private String footerFormat = "<gradient:#ffc663:#ff6000>----------------------------</gradient>";

        @Builder.Default
        private String suffix = "";

        @Builder.Default
        private LeaderboardType leaderboardType = LeaderboardType.TOP_PLAYER_HEAD;

        @Builder.Default
        private boolean showEmptyPlaces = false;

        @Builder.Default
        private int maxDisplayEntries = 10;

        @Builder.Default
        private SortOrder sortOrder = SortOrder.DESCENDING;

        @Builder.Default
        private RotationMode rotationMode = RotationMode.DYNAMIC;

        @Builder.Default
        private HeadMode headMode = HeadMode.ITEM_DISPLAY;

        @Builder.Default
        private Map<UUID, String> extra = new HashMap<>();

        @Builder.Default
        private double lineHeight = 0.25;

        @Builder.Default
        private boolean background = true;

        @Builder.Default
        private boolean decimalNumbers = false;

        @Builder.Default
        private int backgroundColor = 0x66000000;
    }

    public record PlayerScore(String name, double score) {}

    /**
     * Configuration options for customizing the leaderboard display.
     * Controls formatting, styling, and display behavior including:
     * - Title and entry formatting
     * - Player head display
     * - Maximum entries shown
     * - Empty place handling
     * - Visual scaling
     */
    @Setter
    @Accessors(chain = true)
    private LeaderboardOptions options;

    public LeaderboardHologram(LeaderboardOptions options, String id) {
        this.options = options;
        validateId(id);
        this.baseId = id;
        this.allTextHolograms = new ArrayList<>();
        this.entryHolograms = new ArrayList<>();

        this.backgroundHologram = new TextHologram(baseId + "_background");
        configureTextHologram(backgroundHologram);
        backgroundHologram.setMiniMessageText(" ");
        backgroundHologram.setBackgroundColor(options.background ? options.backgroundColor : 0);
        this.allTextHolograms.add(backgroundHologram);

        this.headerHologram = new TextHologram(baseId + "_header");
        this.allTextHolograms.add(headerHologram);

        for (int i = 1; i <= options.maxDisplayEntries(); i++) {
            TextHologram entryHologram = new TextHologram(baseId + "_entry_" + i);
            this.entryHolograms.add(entryHologram);
            this.allTextHolograms.add(entryHologram);
        }

        this.footerHologram = new TextHologram(baseId + "_footer");
        this.allTextHolograms.add(footerHologram);

        if(options.leaderboardType == LeaderboardType.TOP_PLAYER_HEAD) {
            this.firstPlaceHead = new ItemHologram(baseId + "_head");
        } else {
            this.firstPlaceHead = null;
        }

        configureAllTextHolograms();
    }

    protected void validateId(String id) {
        if (id.contains(" ")) {
            throw new IllegalArgumentException("The hologram ID cannot contain spaces! (" + id + ")");
        }
    }

    private void configureAllTextHolograms() {
        for (TextHologram hologram : allTextHolograms) {
            configureTextHologram(hologram);
            hologram.setMiniMessageText("");
        }
        backgroundHologram.setMiniMessageText(" ");
        backgroundHologram.setBackgroundColor(options.background ? options.backgroundColor : 0);
    }

    private void configureTextHologram(TextHologram hologram) {
        hologram.setScale(scale, scale, scale)
                .setBackgroundColor(0)
                .setSeeThroughBlocks(false)
                .setAlignment(TextDisplay.TextAlignment.CENTER)
                .setBillboard(options.rotationMode == RotationMode.DYNAMIC ? Display.Billboard.VERTICAL : Display.Billboard.FIXED);
    }

    public void setPlayerScore(UUID uuid, String name, double score) {
        playerData.put(uuid, new PlayerScore(name, score));
    }

    public void setAllScores(Map<UUID, PlayerScore> data) {
        playerData = new HashMap<>(data);
    }

    public void removePlayer(UUID uuid) {
        playerData.remove(uuid);
    }

    public void hide(Player player) {
        for (TextHologram allTextHologram : this.allTextHolograms) {
            allTextHologram.hide(player);
        }
        this.backgroundHologram.hide(player);
    }

    public void show(Player player) {
        this.backgroundHologram.show(player);
        for (TextHologram allTextHologram : this.allTextHolograms) {
            allTextHologram.show(player);
        }
    }

    /**
     * Updates the leaderboard display with current data
     * Formats and displays the leaderboard entries according to the specified options,
     * including special formatting for top 3 places and optional player head display
     */
    public void update() {
        if (baseLocation == null) {
            Bukkit.getLogger().log(Level.WARNING, "Base location not set for leaderboard hologram");
            return;
        }

        this.leaderboardEntries = playerData.size();
        List<Map.Entry<UUID, PlayerScore>> sorted = getSortedEntries();

        double currentY = 0;

        updateFooterHologram(currentY);
        currentY += options.lineHeight();

        int maxEntries = options.maxDisplayEntries();

        if (options.showEmptyPlaces()) {
            for (int i = maxEntries - 1; i >= sorted.size(); i--) {
                TextHologram entryHologram = entryHolograms.get(i);
                int place = i + 1;
                entryHologram.setMiniMessageText(getEmptyPlaceFormat(place));
                positionHologram(entryHologram, currentY);
                currentY += options.lineHeight();
            }
        } else {
            for (int i = sorted.size(); i < maxEntries; i++) {
                TextHologram entryHologram = entryHolograms.get(i);
                entryHologram.setMiniMessageText("");
                entryHologram.teleport(baseLocation);
            }
        }

        for (int i = Math.min(sorted.size(), maxEntries) - 1; i >= 0; i--) {
            TextHologram entryHologram = entryHolograms.get(i);
            int place = i + 1;

            Map.Entry<UUID, PlayerScore> entry = sorted.get(i);
            UUID uuid = entry.getKey();
            PlayerScore playerScore = entry.getValue();
            String content = getFormattedEntry(place, uuid, playerScore);
            entryHologram.setMiniMessageText(content);
            positionHologram(entryHologram, currentY);
            currentY += options.lineHeight();
        }

        if (options.leaderboardType() == LeaderboardType.TOP_PLAYER_HEAD && !sorted.isEmpty()) {
            updateFirstPlaceHead(sorted.get(0).getKey(), currentY);
            currentY += options.lineHeight() * 5;
        }

        currentY += options.lineHeight();
        updateHeaderHologram(currentY);

        for (int i = 0; i < maxEntries; i++) {
            TextHologram entryHologram = entryHolograms.get(i);
            entryHologram.update();
        }

        updateBackgroundHologram(currentY);
    }

    private void updateFirstPlaceHead(UUID uuid, double yOffset) {
        if (firstPlaceHead == null) return;

        try {
            this.firstPlaceHead.setPlayerHead(uuid)
                    .setScale(2 * scale, 2 * scale, 0.01f * scale)
                    .setBillboard(options.rotationMode == RotationMode.DYNAMIC ? Display.Billboard.VERTICAL : Display.Billboard.FIXED);

            this.firstPlaceHead.teleport(this.baseLocation);

            this.firstPlaceHead.setTranslation(0F, (float) yOffset + 0.7F, 0F);
            this.firstPlaceHead.update();
        } catch (Exception e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to update first place head: " + e.getMessage());
        }
    }

    private void updateBackgroundHologram(double currentY) {
        float totalHeight = (float) (currentY + 0.8) * 4;
        float bgWidth = options.backgroundWidth * scale;
        float bgHeight = totalHeight * scale;
        backgroundHologram.setTranslation(-0.5F, (float) -options.lineHeight(), 0);
        backgroundHologram.setScale(bgWidth, bgHeight, 1f);
        backgroundHologram.teleport(baseLocation);
        backgroundHologram.update();
    }

    private float calculateTotalHeight() {
        float offset = options.showEmptyPlaces() ? options.maxDisplayEntries() : Math.min(leaderboardEntries, options.maxDisplayEntries()) + 4;
        if (options.leaderboardType() == LeaderboardType.TOP_PLAYER_HEAD) {
            offset += 5;
        }
        return (float) (offset * options.lineHeight());
    }

    private void updateHeaderHologram(double yOffset) {
        String headerText = options.titleFormat().replace("{title}", options.title());
        headerHologram.setMiniMessageText(headerText);
        positionHologram(headerHologram, yOffset);
        headerHologram.update();
    }

    private void updateFooterHologram(double yOffset) {
        footerHologram.setMiniMessageText(options.footerFormat());
        positionHologram(footerHologram, yOffset);
        footerHologram.update();
    }

    private void positionHologram(TextHologram hologram, double yOffset) {
        Location pos = baseLocation.clone().add(0, yOffset, 0);
        hologram.teleport(pos);
    }

    private List<Map.Entry<UUID, PlayerScore>> getSortedEntries() {
        List<Map.Entry<UUID, PlayerScore>> sorted = new ArrayList<>(playerData.entrySet());
        Comparator<Map.Entry<UUID, PlayerScore>> comparator = Comparator.comparingDouble(
                entry -> entry.getValue().score()
        );

        if (options.sortOrder() == SortOrder.DESCENDING) {
            comparator = comparator.reversed();
        }

        sorted.sort(comparator);
        return sorted.subList(0, Math.min(sorted.size(), options.maxDisplayEntries()));
    }

    private String getFormattedEntry(int place, UUID uuid, PlayerScore playerScore) {
        String placeFormat = place <= 3 && place <= options.placeFormats().length
                ? options.placeFormats()[place - 1]
                : options.defaultPlaceFormat();

        String headText = "";
        if (options.leaderboardType() == LeaderboardType.ALL_PLAYER_HEADS &&
                options.headMode() == HeadMode.RESOURCEPACK) {
            headText = PlayerUtils.getPlayerHead(uuid);
        }

        String formattedEntry = placeFormat
                .replace("{place}", String.valueOf(place))
                .replace("{name}", playerScore.name())
                .replace("{score}", String.valueOf(options.decimalNumbers ? playerScore.score() : ((long) playerScore.score())))
                .replace("{suffix}", options.suffix())
                .replace("{extra}", options.extra().getOrDefault(uuid, ""))
                .replace("{head}", headText);

        return formattedEntry;
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

    @ApiStatus.Experimental
    public LeaderboardHologram rotate(float x, float y) {
        for (TextHologram hologram : allTextHolograms) {
            hologram.setRotation(x, y).update();
        }
        if (firstPlaceHead != null) {
            firstPlaceHead.setRotation(x, y).update();
        }
        return this;
    }

    public LeaderboardHologram setFixedRotation() {
        for (TextHologram hologram : allTextHolograms) {
            hologram.setBillboard(Display.Billboard.FIXED);
        }
        if (firstPlaceHead != null) {
            firstPlaceHead.setBillboard(Display.Billboard.FIXED);
        }
        return this;
    }

    public LeaderboardHologram teleport(Location location) {
        this.baseLocation = location.clone();
        update();
        return this;
    }

    public Location getLocation() {
        return baseLocation != null ? baseLocation.clone() : null;
    }

    public List<TextHologram> getAllTextHolograms() {
        return new ArrayList<>(allTextHolograms);
    }

    public List<TextHologram> getEntryHolograms() {
        return new ArrayList<>(entryHolograms);
    }
}