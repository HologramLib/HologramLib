package com.maximde.hologramlib.hologram.custom;

import com.maximde.hologramlib.hologram.InteractionBox;
import com.maximde.hologramlib.hologram.TextHologram;
import com.maximde.hologramlib.utils.Vector3F;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Getter
@ApiStatus.Experimental
public class PagedLeaderboard {

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
    private String leftArrowText = "<gold>◀</gold>";
    private String rightArrowText = "<gold>▶</gold>";
    private Vector3F interactionBoxSize = new Vector3F(0.7F, 1, 0);

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
                .setScale(4, 5, 1)
                .setBillboard(Display.Billboard.FIXED);

        rightArrow = new TextHologram(baseId + "_right_arrow");
        rightArrow.setMiniMessageText(rightArrowText)
                .setScale(4, 5, 1)
                .setBillboard(Display.Billboard.FIXED);


        leftInteraction = new InteractionBox(baseId + "_left_interact", this::previousPage);
        leftInteraction.setSize(interactionBoxSize)
                .setResponsive(true);

        rightInteraction = new InteractionBox(baseId + "_right_interact", this::nextPage);
        rightInteraction.setSize(interactionBoxSize)
                .setResponsive(true);
    }

    public PagedLeaderboard rotate(float x) {
        for (LeaderboardHologram page : pages) {
            page.rotate(x, 0);
        }

        leftArrow.setRotation(x, 0).update();
        rightArrow.setRotation(x, 0).update();
        return this;
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

    /**
     * Compute a world-space offset from baseLocation so that
     * +offset sits to the right of the forward direction, and
     * –offset sits to the left.
     */
    private Location computeArrowLocation(double offset) {
        float yawDeg = pages.get(0).getHeaderHologram().getRotation().x;
        double yawRad = Math.toRadians(yawDeg);

        double dx =  Math.cos(yawRad) * offset;
        double dz =  Math.sin(yawRad) * offset;

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

            LeaderboardHologram firstPage = pages.get(0);
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                firstPage.show(player);
                playerCurrentPage.put(player.getUniqueId(), 0);
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
        player.playSound(rightInteraction.getLocation(), Sound.BLOCK_HONEY_BLOCK_BREAK, 1F, 1F);
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
        player.playSound(leftInteraction.getLocation(), Sound.BLOCK_HONEY_BLOCK_BREAK, 1F, 1F);
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

        leftArrow.show(player);
        rightArrow.show(player);
        leftInteraction.addViewer(player);
        rightInteraction.addViewer(player);

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

        leftArrow.hide(player);
        rightArrow.hide(player);
        leftInteraction.removeViewer(player);
        rightInteraction.removeViewer(player);

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

}