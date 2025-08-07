package com.maximde.hologramlib.hologram.custom;

import com.maximde.hologramlib.hologram.HologramManager;
import com.maximde.hologramlib.hologram.InteractionBox;
import com.maximde.hologramlib.hologram.TextHologram;
import com.maximde.hologramlib.utils.Vector3F;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@ApiStatus.Experimental
public class PagedLeaderboard implements HologramManager.Events {

    private final List<LeaderboardHologram> pages = new ArrayList<>();
    private final Map<UUID, Integer> playerCurrentPage = new HashMap<>();
    @Getter
    private final String baseId;

    private TextHologram leftArrow;
    private TextHologram rightArrow;
    private InteractionBox leftInteraction;
    private InteractionBox rightInteraction;

    private Location baseLocation;
    @Getter
    private boolean spawned = false;

    private double arrowOffset = 3;
    private double arrowHeight = 1.0;
    private String leftArrowText = "<gold><</gold>";
    private String rightArrowText = "<gold>></gold>";
    private Vector3F interactionBoxSize = new Vector3F(0.7F, 1, 0);

    @Getter
    private Sound leftClickSound = Sound.BLOCK_AMETHYST_CLUSTER_FALL;
    @Getter
    private Sound rightClickSound = Sound.BLOCK_AMETHYST_CLUSTER_FALL;
    @Getter
    private float leftClickVolume = 1.0F;
    @Getter
    private float rightClickVolume = 1.0F;
    @Getter
    private float leftClickPitch = 1.0F;
    @Getter
    private float rightClickPitch = 1.0F;

    @Getter
    private int leftArrowBackground = 0;
    @Getter
    private int rightArrowBackground = 0;
    @Getter
    private Vector3F leftArrowScale = new Vector3F(4, 5, 1);
    @Getter
    private Vector3F rightArrowScale = new Vector3F(4, 5, 1);

    public PagedLeaderboard(String baseId) {
        validateId(baseId);
        this.baseId = baseId;
        initializeArrows();
    }

    protected void validateId(String id) {
        if (id.contains(" ")) {
            throw new IllegalArgumentException("The hologram ID cannot contain spaces! (" + id + ")");
        }
    }

    private void initializeArrows() {
        leftArrow = new TextHologram(baseId + "_left_arrow");
        leftArrow.setMiniMessageText(leftArrowText)
                .setScale(leftArrowScale.x, leftArrowScale.y, leftArrowScale.z)
                .setBackgroundColor(leftArrowBackground)
                .setBillboard(Display.Billboard.FIXED);

        rightArrow = new TextHologram(baseId + "_right_arrow");
        rightArrow.setMiniMessageText(rightArrowText)
                .setScale(rightArrowScale.x, rightArrowScale.y, rightArrowScale.z)
                .setBackgroundColor(rightArrowBackground)
                .setBillboard(Display.Billboard.FIXED);

        leftInteraction = new InteractionBox(baseId + "_left_interact", this::previousPage);
        leftInteraction.setSize(interactionBoxSize)
                .setResponsive(true);

        rightInteraction = new InteractionBox(baseId + "_right_interact", this::nextPage);

        rightInteraction.setSize(interactionBoxSize)
                .setResponsive(true);
    }

    @ApiStatus.Experimental
    public PagedLeaderboard rotate(float x) {
        for (LeaderboardHologram page : pages) {
            page.rotate(x, 0);
        }

        leftArrow.setRotation(x, 0).update();
        rightArrow.setRotation(x, 0).update();
        return this;
    }

    @Override
    public void onJoin(Player player) {
        if (!this.playerCurrentPage.containsKey(player.getUniqueId())) {
            this.show(player);
        }
    }

    @Override
    public void onQuit(Player player) {
        playerCurrentPage.remove(player.getUniqueId());
    }

    /**
     * Spawns the paged leaderboard at the specified location
     */
    public void init(Location location) {

        if (spawned) {
            throw new IllegalStateException("PagedLeaderboard is already spawned!");
        }

        if (pages.isEmpty()) {
            throw new IllegalStateException("No pages added to the leaderboard!");
        }

        this.baseLocation = location.clone();
        spawned = true;

        for (LeaderboardHologram page : pages) {
            page.teleport(baseLocation);
        }

        Location leftLoc  = computeArrowLocation(-arrowOffset);
        Location rightLoc = computeArrowLocation( arrowOffset);

        leftArrow.teleport(leftLoc).update();
        rightArrow.teleport(rightLoc).update();

        leftInteraction.teleport(leftLoc);
        rightInteraction.teleport(rightLoc);


        showInitialPage();
    }

    private Location computeArrowLocation(double offset) {
        float yawDeg = pages.get(0).getXRotation();

        double yawRad = Math.toRadians(yawDeg);

        double dx =  Math.cos(yawRad) * offset;
        double dz = -Math.sin(yawRad) * offset;

        return baseLocation.clone().add(dx, arrowHeight, dz);
    }


    /**
     * Shows the initial page (page 0) to all online players
     */
    private void showInitialPage() {
        if (!pages.isEmpty()) {
            for (LeaderboardHologram page : pages) {
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    page.hide(player);
                }
            }


            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                playerCurrentPage.put(player.getUniqueId(), 0);
                pages.get(0).show(player);
            }
        }
    }

    /**
     * Goes to the next page for the specified player
     */
    public void nextPage(Player player) {
        if (!spawned || pages.isEmpty()) return;

        UUID playerId = player.getUniqueId();
        int currentPage = playerCurrentPage.getOrDefault(playerId, 0);
        int nextPage = (currentPage + 1) % pages.size();

        switchToPage(player, nextPage);
        player.playSound(rightInteraction.getLocation(), rightClickSound, rightClickVolume, rightClickPitch);
    }

    /**
     * Goes to the previous page for the specified player
     */
    public void previousPage(Player player) {
        if (!spawned || pages.isEmpty()) return;

        UUID playerId = player.getUniqueId();
        int currentPage = playerCurrentPage.getOrDefault(playerId, 0);
        int prevPage = (currentPage - 1 + pages.size()) % pages.size();

        switchToPage(player, prevPage);
        player.playSound(leftInteraction.getLocation(), leftClickSound, leftClickVolume, leftClickPitch);
    }

    public PagedLeaderboard setLeftClickSound(Sound sound) {
        this.leftClickSound = sound;
        return this;
    }

    public PagedLeaderboard setRightClickSound(Sound sound) {
        this.rightClickSound = sound;
        return this;
    }

    public PagedLeaderboard setLeftClickSound(Sound sound, float volume, float pitch) {
        this.leftClickSound = sound;
        this.leftClickVolume = volume;
        this.leftClickPitch = pitch;
        return this;
    }

    public PagedLeaderboard setRightClickSound(Sound sound, float volume, float pitch) {
        this.rightClickSound = sound;
        this.rightClickVolume = volume;
        this.rightClickPitch = pitch;
        return this;
    }

    public PagedLeaderboard setClickSounds(Sound leftSound, Sound rightSound) {
        this.leftClickSound = leftSound;
        this.rightClickSound = rightSound;
        return this;
    }

    public PagedLeaderboard setClickSounds(Sound leftSound, Sound rightSound, float volume, float pitch) {
        this.leftClickSound = leftSound;
        this.rightClickSound = rightSound;
        this.leftClickVolume = volume;
        this.rightClickVolume = volume;
        this.leftClickPitch = pitch;
        this.rightClickPitch = pitch;
        return this;
    }

    /**
     * Switches the player to a specific page
     */
    public void switchToPage(Player player, int pageIndex) {
        if (!spawned || pages.isEmpty() || pageIndex < 0 || pageIndex >= pages.size()) {
            return;
        }

        UUID playerId = player.getUniqueId();
        int currentPage = playerCurrentPage.getOrDefault(playerId, 0);

        if (currentPage == pageIndex) return;

        if (currentPage < pages.size()) {
            pages.get(currentPage).hide(player);
        }

        pages.get(pageIndex).show(player);
        playerCurrentPage.put(playerId, pageIndex);
    }

    /**
     * Gets the current page index for a player
     */
    public int getCurrentPage(Player player) {
        return playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
    }

    /**
     * Adds a new leaderboard page
     */
    public PagedLeaderboard addPage(LeaderboardHologram page) {
        pages.add(page);
        if (spawned) {
            page.teleport(baseLocation);

            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                page.hide(player);
            }
        }
        return this;
    }

    /**
     * Removes a page by index
     */
    public boolean removePage(int index) {
        if (index < 0 || index >= pages.size()) return false;

        LeaderboardHologram removedPage = pages.remove(index);

        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            removedPage.hide(player);
        }

        playerCurrentPage.entrySet().removeIf(entry -> {
            int playerPage = entry.getValue();
            if (playerPage == index) {
                Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
                if (player != null && !pages.isEmpty()) {
                    switchToPage(player, 0);
                }
                return true;
            } else if (playerPage > index) {
                entry.setValue(playerPage - 1);
            }
            return false;
        });

        return true;
    }

    /**
     * Updates all leaderboard pages
     */
    public void updateAllPages() {
        for (LeaderboardHologram page : pages) {
            page.update();
        }
    }

    /**
     * Shows the paged leaderboard to a specific player
     */
    public void show(Player player) {
        if (!spawned) return;

        for (LeaderboardHologram page : pages) {
            page.hide(player);
        }

        leftArrow.show(player);
        rightArrow.show(player);
        leftInteraction.show(player);
        rightInteraction.show(player);

        int currentPage = playerCurrentPage.getOrDefault(player.getUniqueId(), 0);
        if (currentPage < pages.size()) {
            pages.get(currentPage).show(player);
        } else if (!pages.isEmpty()) {
            switchToPage(player, 0);
        }
    }

    /**
     * Hides the paged leaderboard from a specific player
     */
    public void hide(Player player) {
        if (!spawned) return;

        leftInteraction.hide(player);
        rightInteraction.hide(player);
        leftArrow.hide(player);
        rightArrow.hide(player);

        for (LeaderboardHologram page : pages) {
            page.hide(player);
        }
    }

    public PagedLeaderboard setArrowOffset(double arrowOffset) {
        this.arrowOffset = arrowOffset;
        return this;
    }

    public PagedLeaderboard setArrowHeight(double arrowHeight) {
        this.arrowHeight = arrowHeight;
        return this;
    }

    public PagedLeaderboard setLeftArrowText(String leftArrowText) {
        this.leftArrowText = leftArrowText;
        if (leftArrow != null) {
            leftArrow.setMiniMessageText(leftArrowText).update();
        }
        return this;
    }

    public PagedLeaderboard setRightArrowText(String rightArrowText) {
        this.rightArrowText = rightArrowText;
        if (rightArrow != null) {
            rightArrow.setMiniMessageText(rightArrowText).update();
        }
        return this;
    }

    public PagedLeaderboard setInteractionBoxSize(Vector3F interactionBoxSize) {
        this.interactionBoxSize = interactionBoxSize;
        if (leftInteraction != null) {
            leftInteraction.setSize(interactionBoxSize);
        }
        if (rightInteraction != null) {
            rightInteraction.setSize(interactionBoxSize);
        }
        return this;
    }

    public List<LeaderboardHologram> getPages() {
        return new ArrayList<>(pages);
    }

    public int getPageCount() {
        return pages.size();
    }

    public Location getLocation() {
        return baseLocation != null ? baseLocation.clone() : null;
    }

    public PagedLeaderboard setArrowBackgrounds(int backgroundColor) {
        this.leftArrowBackground = backgroundColor;
        this.rightArrowBackground = backgroundColor;
        if (leftArrow != null) {
            leftArrow.setBackgroundColor(backgroundColor).update();
        }
        if (rightArrow != null) {
            rightArrow.setBackgroundColor(backgroundColor).update();
        }
        return this;
    }

    public PagedLeaderboard setLeftArrowBackground(int backgroundColor) {
        this.leftArrowBackground = backgroundColor;
        if (leftArrow != null) {
            leftArrow.setBackgroundColor(backgroundColor).update();
        }
        return this;
    }

    public PagedLeaderboard setRightArrowBackground(int backgroundColor) {
        this.rightArrowBackground = backgroundColor;
        if (rightArrow != null) {
            rightArrow.setBackgroundColor(backgroundColor).update();
        }
        return this;
    }

    public PagedLeaderboard setArrowScale(Vector3F scale) {
        this.leftArrowScale = scale;
        this.rightArrowScale = scale;
        if (leftArrow != null) {
            leftArrow.setScale(scale.x, scale.y, scale.z).update();
        }
        if (rightArrow != null) {
            rightArrow.setScale(scale.x, scale.y, scale.z).update();
        }
        return this;
    }

    public PagedLeaderboard setArrowScale(float x, float y, float z) {
        return setArrowScale(new Vector3F(x, y, z));
    }

    public PagedLeaderboard setLeftArrowScale(Vector3F scale) {
        this.leftArrowScale = scale;
        if (leftArrow != null) {
            leftArrow.setScale(scale.x, scale.y, scale.z).update();
        }
        return this;
    }

    public PagedLeaderboard setLeftArrowScale(float x, float y, float z) {
        return setLeftArrowScale(new Vector3F(x, y, z));
    }

    public PagedLeaderboard setRightArrowScale(Vector3F scale) {
        this.rightArrowScale = scale;
        if (rightArrow != null) {
            rightArrow.setScale(scale.x, scale.y, scale.z).update();
        }
        return this;
    }

    public PagedLeaderboard setRightArrowScale(float x, float y, float z) {
        return setRightArrowScale(new Vector3F(x, y, z));
    }
}