package com.maximde.hologramlib.hologram;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.maximde.hologramlib.utils.PlayerUtils;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;


import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class LeaderboardHologram {

    private final TextHologram textHologram;

    private final ItemHologram firstPlaceHead;

    private int leaderboardEntries = 0;

    @Data
    @Builder
    @Accessors(fluent = true)
    public static class LeaderboardOptions {

        @Builder.Default
        private String title = "Leaderboard";

        @Builder.Default
        private String[] placeFormats = new String[] {
                "<color:#fdcc00><bold>1. </bold>{name}</color> <gray>{score}</gray> <white>{suffix}</white>",
                "<color:#dcdcdc><bold>2. </bold>{name}</color> <gray>{score}</gray> <white>{suffix}</white>",
                "<color:#e65f2f><bold>3. </bold>{name}</color> <gray>{score}</gray> <white>{suffix}</white>"
        };

        @Builder.Default
        private String defaultPlaceFormat = "<color:#ffb486><bold>{place}. </bold>{name}</color> <gray>{score}</gray> <white>{suffix}</white>";

        @Builder.Default
        private String titleFormat = "<gradient:#ff6000:#ffa42a>▛▀▀▀▀ {title} ▀▀▀▀▜</gradient>";

        @Builder.Default
        private String footerFormat = "<color:#ff6000>▙▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▟</color>";

        @Builder.Default
        private String suffix = "";

        @Builder.Default
        private float scale = 1.0f;

        @Builder.Default
        private boolean topPlayerHead = true;

        @Builder.Default
        private boolean showEmptyPlaces = false;

        @Builder.Default
        private int maxDisplayEntries = 10;
    }

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

    public LeaderboardHologram(LeaderboardOptions options) {
        this.options = options;
        this.textHologram = new TextHologram(
                "leaderboard_" + UUID.randomUUID()
        );

        this.firstPlaceHead = new ItemHologram(textHologram.getId() + "_head");
    }

    /**
     * Updates the leaderboard display with new data.
     * @param leaderboardData Map of position -> "playername:score" entries and the keys (Integer) in the map are used for the players ranking in the leaderboard
     * @param leaderboardOptions Configuration options for the display
     * Formats and displays the leaderboard entries according to the specified options,
     * including special formatting for top 3 places and optional player head display.
     */
    public void updateLeaderboard(Map<Integer, String> leaderboardData, LeaderboardOptions leaderboardOptions) {
        this.options = leaderboardOptions;
        this.leaderboardEntries = leaderboardData.size();
        Map<Integer, String> sortedData = new LinkedHashMap<>(leaderboardData);
        StringBuilder leaderboardText = new StringBuilder();

        String space = options.topPlayerHead() ? "\n\n\n\n\n" : "";

        leaderboardText.append(
                options.titleFormat()
                        .replace("{title}", options.title())
        ).append("\n\n").append(space);

        int maxEntries = options.maxDisplayEntries();
        for (int i = 1; i <= maxEntries; i++) {
            String entryValue = sortedData.getOrDefault(i, "");

            if (!options.showEmptyPlaces() && entryValue.isEmpty()) {
                continue;
            }

            if (entryValue.isEmpty()) {
                entryValue = "Unknown:N/A";
            }

            String[] parts = entryValue.split(":");
            String name = parts.length > 0 ? parts[0] : "Unknown";
            String score = parts.length > 1 ? parts[1] : "N/A";

            String placeFormat = i <= 3 && i <= options.placeFormats().length
                    ? options.placeFormats()[i - 1]
                    : options.defaultPlaceFormat();

            leaderboardText.append(
                    placeFormat
                            .replace("{place}", String.valueOf(i))
                            .replace("{name}", name)
                            .replace("{score}", score)
                            .replace("{suffix}", options.suffix())
            ).append("\n");
        }

        leaderboardText.append("\n").append(options.footerFormat());

        this.textHologram
                .setScale(options.scale(), options.scale(), options.scale())
                .setBackgroundColor(520093695)
                .setAlignment(TextDisplay.TextAlignment.CENTER)
                .setBillboard(Display.Billboard.VERTICAL);

        UUID topPlayerUUID = Optional.ofNullable(sortedData.get(1))
                .map(data -> data.split(":")[0])
                .flatMap(PlayerUtils::getUUID)
                .orElse(UUID.randomUUID());



        if (options.topPlayerHead()) {
            updateFirstPlaceHead(topPlayerUUID);
        }

        textHologram.setMiniMessageText(leaderboardText.toString());

        textHologram.update();
    }


    private void updateFirstPlaceHead(UUID uuid) {
        try {
            ItemStack headItem;
            List<ItemProfile.Property> properties = new ArrayList<>();
            properties.add(new ItemProfile.Property(
                    "textures",
                    Base64.getEncoder().encodeToString(("{\"textures\":{\"SKIN\":{\"url\":\"" + PlayerUtils.getPlayerSkinUrl(uuid) + "\"}}}").getBytes()),
                    null));
            headItem = new ItemStack.Builder()
                    .type(ItemTypes.PLAYER_HEAD)
                    .component(ComponentTypes.PROFILE, new ItemProfile("none", uuid, properties))
                    .build();

            //TODO < 1.20.5 impl

            this.firstPlaceHead.setItem(headItem);
            this.firstPlaceHead.setScale(2 * options.scale, 2 * options.scale, 0.01f * options.scale);
            this.firstPlaceHead.setGlowing(true);
            this.firstPlaceHead.setGlowColor(Color.ORANGE);
            this.firstPlaceHead.setBillboard(Display.Billboard.VERTICAL);

            float baseTranslation = 1.3f;

            float dynamicOffset;

            if (options.showEmptyPlaces()) {
                dynamicOffset = options.maxDisplayEntries();
            } else {
                dynamicOffset = Math.min(leaderboardEntries, options.maxDisplayEntries());
            }
            dynamicOffset = dynamicOffset * 0.25f;

            this.firstPlaceHead.setTranslation(0F, (baseTranslation + dynamicOffset) * options.scale(), 0F);

            this.firstPlaceHead.update();
        } catch (Exception exception) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to update the first place playerhead in hologram! Error: " + exception.getMessage());
            Bukkit.getLogger().log(Level.WARNING, exception.getMessage());
        }
    }

    public LeaderboardHologram teleport(Location location) {
        this.textHologram.teleport(location);
        this.firstPlaceHead.teleport(location);
        return this;
    }

    public Location getLocation() {
        return textHologram.getLocation();
    }


}