package com.maximde.hologramlib.hologram.custom;

import com.maximde.hologramlib.hologram.HologramManager;
import com.maximde.hologramlib.hologram.InteractionBox;
import com.maximde.hologramlib.hologram.TextHologram;
import com.maximde.hologramlib.utils.BukkitTasks;
import com.maximde.hologramlib.utils.Vector3F;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * An advanced leaderboard system that supports:
 * - Multiple stat modes (e.g., Wins, Kills, Deaths)
 * - Customizable state buttons with any number of states
 * - Independent pagination for each stat mode
 * - Flexible button positioning (above, below, beside the leaderboard)
 * - Multiple state buttons that can be combined
 */
@Getter
@ApiStatus.Experimental
public class SwitchableLeaderboard implements HologramManager.Events {

    private final String baseId;
    private final Map<String, StatMode> statModes = new LinkedHashMap<>();
    private final List<StateButton> stateButtons = new ArrayList<>();

    private final Map<UUID, PlayerState> playerStates = new HashMap<>();

    private Location baseLocation;
    private boolean spawned = false;

    private TextHologram leftArrow;
    private TextHologram rightArrow;
    private InteractionBox leftInteraction;
    private InteractionBox rightInteraction;

    private double arrowOffset = 3;
    private double arrowHeight = 1.0;
    private String leftArrowText = "<gold><</gold>";
    private String rightArrowText = "<gold>></gold>";
    private Vector3F interactionBoxSize = new Vector3F(0.7F, 1, 0);

    @Setter @Accessors(chain = true)
    private boolean showArrows = true;

    @Setter @Accessors(chain = true)
    private boolean singleDirectionCycling = false;

    @Getter
    private Sound navigationSound = Sound.BLOCK_AMETHYST_CLUSTER_FALL;
    @Getter
    private float navigationVolume = 1.0F;
    @Getter
    private float navigationPitch = 1.0F;

    @Getter
    private Sound stateChangeSound = Sound.BLOCK_NOTE_BLOCK_PLING;
    @Getter
    private float stateChangeVolume = 1.0F;
    @Getter
    private float stateChangePitch = 1.5F;

    @Getter
    private int arrowBackground = 0;
    @Getter
    private Vector3F arrowScale = new Vector3F(4, 5, 1);

    /**
     * Represents a player's current state across all state dimensions
     */
    private static class PlayerState {
        private final Map<String, Integer> buttonStates = new HashMap<>();
        @Getter
        @Setter
        private String currentStatMode;
        @Setter
        @Getter
        private int currentPage = 0;

        public PlayerState(String initialStatMode) {
            this.currentStatMode = initialStatMode;
        }

        public int getButtonState(String buttonId) {
            return buttonStates.getOrDefault(buttonId, 0);
        }

        public void setButtonState(String buttonId, int stateIndex) {
            buttonStates.put(buttonId, stateIndex);
        }

    }

    /**
     * Represents a stat mode (e.g., "Wins", "Kills", "Deaths")
     * Each stat mode has its own set of pages for pagination
     */
    @Getter
    public static class StatMode {
        private final String id;
        private final String displayName;
        private final List<LeaderboardHologram> pages = new ArrayList<>();
        private final Map<String, Object> metadata = new HashMap<>();

        public StatMode(String id, String displayName) {
            validateId(id);
            this.id = id;
            this.displayName = displayName;
        }

        public StatMode addPage(LeaderboardHologram page) {
            pages.add(page);
            return this;
        }

        public StatMode setMetadata(String key, Object value) {
            metadata.put(key, value);
            return this;
        }

        public Object getMetadata(String key) {
            return metadata.get(key);
        }

        public int getPageCount() {
            return pages.size();
        }

        private static void validateId(String id) {
            if (id.contains(" ")) {
                throw new IllegalArgumentException("Stat mode ID cannot contain spaces: " + id);
            }
        }
    }

    /**
     * Represents a button that cycles through multiple states
     * Can be positioned anywhere relative to the leaderboard
     */
    @Getter
    public static class StateButton {
        private final String id;
        private final List<ButtonState> states;
        private final Vector3F relativePosition;
        private final Vector3F scale;
        private final int backgroundColor;
        private final Display.Billboard billboard;

        private TextHologram hologram;
        private InteractionBox interaction;

        private List<TextHologram> stateHolograms = new ArrayList<>();
        public List<TextHologram> getStateHolograms() { return stateHolograms; }
        void setStateHolograms(List<TextHologram> stateHolograms) { this.stateHolograms = stateHolograms; }

        /**
         * Callback when button state changes
         * Parameters: Player, new state index
         */
        private BiConsumer<Player, Integer> onStateChange;

        public static class Builder {
            private final String id;
            private final List<ButtonState> states = new ArrayList<>();
            private Vector3F relativePosition = new Vector3F(0, -2, 0);
            private Vector3F scale = new Vector3F(3, 3, 1);
            private int backgroundColor = 0;
            private Display.Billboard billboard = Display.Billboard.FIXED;
            private BiConsumer<Player, Integer> onStateChange;

            public Builder(String id) {
                validateId(id);
                this.id = id;
            }

            public Builder addState(String displayText) {
                this.states.add(new ButtonState(displayText, null));
                return this;
            }

            public Builder addState(String displayText, Object data) {
                this.states.add(new ButtonState(displayText, data));
                return this;
            }

            public Builder setRelativePosition(double x, double y, double z) {
                this.relativePosition = new Vector3F((float) x, (float) y, (float) z);
                return this;
            }

            public Builder setRelativePosition(Vector3F position) {
                this.relativePosition = position;
                return this;
            }

            public Builder setScale(float x, float y, float z) {
                this.scale = new Vector3F(x, y, z);
                return this;
            }

            public Builder setScale(Vector3F scale) {
                this.scale = scale;
                return this;
            }

            public Builder setBackgroundColor(int color) {
                this.backgroundColor = color;
                return this;
            }

            public Builder setBillboard(Display.Billboard billboard) {
                this.billboard = billboard;
                return this;
            }

            public Builder onStateChange(BiConsumer<Player, Integer> callback) {
                this.onStateChange = callback;
                return this;
            }

            public StateButton build() {
                if (states.isEmpty()) {
                    throw new IllegalStateException("Button must have at least one state");
                }
                return new StateButton(id, states, relativePosition, scale, backgroundColor, billboard, onStateChange);
            }

            private static void validateId(String id) {
                if (id.contains(" ")) {
                    throw new IllegalArgumentException("Button ID cannot contain spaces: " + id);
                }
            }
        }

        private StateButton(String id, List<ButtonState> states, Vector3F relativePosition,
                            Vector3F scale, int backgroundColor, Display.Billboard billboard,
                            BiConsumer<Player, Integer> onStateChange) {
            this.id = id;
            this.states = new ArrayList<>(states);
            this.relativePosition = relativePosition;
            this.scale = scale;
            this.backgroundColor = backgroundColor;
            this.billboard = billboard;
            this.onStateChange = onStateChange;
        }

        public ButtonState getState(int index) {
            if (index < 0 || index >= states.size()) {
                return states.get(0);
            }
            return states.get(index);
        }

        public int getStateCount() {
            return states.size();
        }

        void setHologram(TextHologram hologram) {
            this.hologram = hologram;
        }

        void setInteraction(InteractionBox interaction) {
            this.interaction = interaction;
        }
    }

    /**
     * Represents a single state of a button
     */
    @Getter
    public static class ButtonState {
        private final String displayText;
        private final Object data;

        public ButtonState(String displayText, Object data) {
            this.displayText = displayText;
            this.data = data;
        }
    }

    public SwitchableLeaderboard(String baseId) {
        validateId(baseId);
        this.baseId = baseId;
        initializeNavigationArrows();
    }

    protected void validateId(String id) {
        if (id.contains(" ")) {
            throw new IllegalArgumentException("The leaderboard ID cannot contain spaces! (" + id + ")");
        }
    }

    private void initializeNavigationArrows() {
        leftArrow = new TextHologram(baseId + "_left_arrow");
        leftArrow.setMiniMessageText(leftArrowText)
                .setScale(arrowScale.x, arrowScale.y, arrowScale.z)
                .setBackgroundColor(arrowBackground)
                .setBillboard(Display.Billboard.FIXED);

        rightArrow = new TextHologram(baseId + "_right_arrow");
        rightArrow.setMiniMessageText(rightArrowText)
                .setScale(arrowScale.x, arrowScale.y, arrowScale.z)
                .setBackgroundColor(arrowBackground)
                .setBillboard(Display.Billboard.FIXED);

        leftInteraction = new InteractionBox(baseId + "_left_interact", this::handleLeftClick);
        leftInteraction.setSize(interactionBoxSize).setResponsive(true);

        rightInteraction = new InteractionBox(baseId + "_right_interact", this::handleRightClick);
        rightInteraction.setSize(interactionBoxSize).setResponsive(true);
    }

    /**
     * Adds a new stat mode to the leaderboard
     * @param statMode The stat mode to add
     * @return This SwitchableLeaderboard for chaining
     */
    public SwitchableLeaderboard addStatMode(StatMode statMode) {
        if (statModes.containsKey(statMode.getId())) {
            throw new IllegalArgumentException("Stat mode with ID " + statMode.getId() + " already exists");
        }
        statModes.put(statMode.getId(), statMode);
        return this;
    }

    /**
     * Adds a state button to the leaderboard
     * @param button The button to add
     * @return This SwitchableLeaderboard for chaining
     */
    public SwitchableLeaderboard addStateButton(StateButton button) {
        if (stateButtons.stream().anyMatch(b -> b.getId().equals(button.getId()))) {
            throw new IllegalArgumentException("Button with ID " + button.getId() + " already exists");
        }

        String baseHologramId = baseId + "_button_" + button.getId();
        List<TextHologram> stateHolos = new ArrayList<>();

        for (int i = 0; i < button.getStateCount(); i++) {
            TextHologram hologram = new TextHologram(baseHologramId + "_" + i);
            hologram.setMiniMessageText(button.getState(i).getDisplayText())
                    .setScale(button.getScale().x, button.getScale().y, button.getScale().z)
                    .setBackgroundColor(button.getBackgroundColor())
                    .setBillboard(button.getBillboard());
            stateHolos.add(hologram);
        }

        button.setHologram(stateHolos.get(0));
        button.setStateHolograms(stateHolos);

        InteractionBox interaction = new InteractionBox(baseHologramId + "_interact",
                player -> handleButtonClick(player, button));
        interaction.setSize(button.getScale().x * 0.5f, button.getScale().y * 0.5f)
                .setResponsive(true);

        button.setInteraction(interaction);

        stateButtons.add(button);
        return this;
    }

    /**
     * Initializes and spawns the leaderboard at the specified location
     */
    public void init(Location location) {
        if (spawned) {
            throw new IllegalStateException("SwitchableLeaderboard is already spawned!");
        }

        if (statModes.isEmpty()) {
            throw new IllegalStateException("No stat modes added to the leaderboard!");
        }

        this.baseLocation = location.clone();
        spawned = true;

        for (StatMode statMode : statModes.values()) {
            for (LeaderboardHologram page : statMode.getPages()) {
                page.setFixedRotation();
                page.teleport(baseLocation);
            }
        }

        if (showArrows) {
            Location leftLoc = computeArrowLocation(-arrowOffset);
            Location rightLoc = computeArrowLocation(arrowOffset);

            leftArrow.teleport(leftLoc).update();
            rightArrow.teleport(rightLoc).update();

            if (!singleDirectionCycling) {
                leftInteraction.teleport(leftLoc);
            }
            rightInteraction.teleport(rightLoc);
        }

        for (StateButton button : stateButtons) {
            Location buttonLoc = baseLocation.clone().add(
                    button.getRelativePosition().x,
                    button.getRelativePosition().y,
                    button.getRelativePosition().z
            );
            for (TextHologram holo : button.getStateHolograms()) {
                holo.teleport(buttonLoc).update();
            }
            button.getInteraction().teleport(buttonLoc);

            BukkitTasks.runTask(() -> {
                for (TextHologram holo : button.getStateHolograms()) {
                    if (holo.isDead()) {
                        holo.getInternalAccess().spawn(buttonLoc, false);
                    }
                }
                for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                    refreshPlayerView(p);
                }
            });
        }

        showInitialState();
    }

    /**
     * Shows the initial state (first stat mode, page 0) to all online players
     */
    private void showInitialState() {
        if (statModes.isEmpty()) return;

        String firstStatModeId = statModes.keySet().iterator().next();

        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            PlayerState state = new PlayerState(firstStatModeId);
            playerStates.put(player.getUniqueId(), state);

            refreshPlayerView(player);
        }
    }

    private void handleLeftClick(Player player) {
        if (singleDirectionCycling) {
            previousPage(player);
        } else {
            previousPage(player);
        }
    }

    private void handleRightClick(Player player) {
        nextPage(player);
    }

    private void handleButtonClick(Player player, StateButton button) {
        PlayerState state = getOrCreatePlayerState(player);

        int currentStateIndex = state.getButtonState(button.getId());
        int nextStateIndex = (currentStateIndex + 1) % button.getStateCount();

        state.setButtonState(button.getId(), nextStateIndex);

        player.playSound(player.getLocation(), stateChangeSound, stateChangeVolume, stateChangePitch);

        if (button.getOnStateChange() != null) {
            button.getOnStateChange().accept(player, nextStateIndex);
        }

        refreshPlayerView(player);
    }

    /**
     * Goes to the next page for the specified player
     */
    public void nextPage(Player player) {
        if (!spawned || statModes.isEmpty()) return;

        PlayerState state = getOrCreatePlayerState(player);
        StatMode currentMode = statModes.get(state.getCurrentStatMode());

        if (currentMode == null || currentMode.getPages().isEmpty()) return;

        int currentPage = state.getCurrentPage();
        int nextPage = (currentPage + 1) % currentMode.getPageCount();

        switchToPage(player, nextPage);

        if (showArrows) {
            player.playSound(rightInteraction.getLocation(), navigationSound, navigationVolume, navigationPitch);
        }
    }

    /**
     * Goes to the previous page for the specified player
     */
    public void previousPage(Player player) {
        if (!spawned || statModes.isEmpty() || singleDirectionCycling) return;

        PlayerState state = getOrCreatePlayerState(player);
        StatMode currentMode = statModes.get(state.getCurrentStatMode());

        if (currentMode == null || currentMode.getPages().isEmpty()) return;

        int currentPage = state.getCurrentPage();
        int prevPage = (currentPage - 1 + currentMode.getPageCount()) % currentMode.getPageCount();

        switchToPage(player, prevPage);

        if (showArrows) {
            player.playSound(leftInteraction.getLocation(), navigationSound, navigationVolume, navigationPitch);
        }
    }

    /**
     * Switches a player to a specific page
     */
    private void switchToPage(Player player, int pageIndex) {
        PlayerState state = getOrCreatePlayerState(player);
        StatMode currentMode = statModes.get(state.getCurrentStatMode());

        if (currentMode == null || pageIndex < 0 || pageIndex >= currentMode.getPageCount()) {
            return;
        }

        int oldPage = state.getCurrentPage();
        if (oldPage >= 0 && oldPage < currentMode.getPageCount() && oldPage != pageIndex) {
            currentMode.getPages().get(oldPage).hide(player);
        }

        currentMode.getPages().get(pageIndex).show(player);
        state.setCurrentPage(pageIndex);
    }

    /**
     * Switches a player to a different stat mode
     * @param player The player
     * @param statModeId The ID of the stat mode to switch to
     */
    public void switchStatMode(Player player, String statModeId) {
        if (!spawned) return;

        StatMode newMode = statModes.get(statModeId);
        if (newMode == null) {
            throw new IllegalArgumentException("Stat mode not found: " + statModeId);
        }

        PlayerState state = getOrCreatePlayerState(player);
        String oldModeId = state.getCurrentStatMode();

        if (oldModeId.equals(statModeId)) {
            return;
        }

        StatMode oldMode = statModes.get(oldModeId);
        if (oldMode != null) {
            for (LeaderboardHologram page : oldMode.getPages()) {
                page.hide(player);
            }
        }

        state.setCurrentStatMode(statModeId);
        state.setCurrentPage(0);

        if (!newMode.getPages().isEmpty()) {
            newMode.getPages().get(0).show(player);
        }
    }

    /**
     * Refreshes the current view for a player based on their state
     */
    public void refreshPlayerView(Player player) {
        if (!spawned) return;

        PlayerState state = getOrCreatePlayerState(player);

        for (StateButton button : stateButtons) {
            int currentBtnState = state.getButtonState(button.getId());
            List<TextHologram> holos = button.getStateHolograms();
            for (int i = 0; i < holos.size(); i++) {
                if (i == currentBtnState) {
                    holos.get(i).show(player);
                } else {
                    holos.get(i).hide(player);
                }
            }
        }

        StatMode currentMode = statModes.get(state.getCurrentStatMode());

        if (currentMode == null) return;

        LeaderboardHologram targetPage = null;
        int currentPage = state.getCurrentPage();
        if (currentPage >= 0 && currentPage < currentMode.getPageCount()) {
            targetPage = currentMode.getPages().get(currentPage);
        }

        for (StatMode statMode : statModes.values()) {
            for (LeaderboardHologram page : statMode.getPages()) {
                if (page != targetPage) {
                    page.hide(player);
                }
            }
        }

        if (targetPage != null) {
            targetPage.show(player);
        }
    }

    /**
     * Gets or creates player state
     */
    private PlayerState getOrCreatePlayerState(Player player) {
        return playerStates.computeIfAbsent(player.getUniqueId(),
                uuid -> new PlayerState(statModes.keySet().iterator().next()));
    }

    /**
     * Shows the leaderboard to a specific player
     */
    public void show(Player player) {
        if (!spawned) return;

        if (showArrows) {
            leftArrow.show(player);
            rightArrow.show(player);

            if (!singleDirectionCycling) {
                leftInteraction.show(player);
            }
            rightInteraction.show(player);
        }

        for (StateButton button : stateButtons) {
            button.getInteraction().show(player);
        }

        refreshPlayerView(player);
    }

    /**
     * Hides the leaderboard from a specific player
     */
    public void hide(Player player) {
        if (!spawned) return;

        if (showArrows) {
            leftArrow.hide(player);
            rightArrow.hide(player);
            leftInteraction.hide(player);
            rightInteraction.hide(player);
        }

        for (StateButton button : stateButtons) {
            for (TextHologram holo : button.getStateHolograms()) {
                holo.hide(player);
            }
            button.getInteraction().hide(player);
        }

        for (StatMode statMode : statModes.values()) {
            for (LeaderboardHologram page : statMode.getPages()) {
                page.hide(player);
            }
        }
    }

    /**
     * Computes the location for navigation arrows based on offset
     */
    private Location computeArrowLocation(double offset) {
        if (statModes.isEmpty()) {
            return baseLocation.clone().add(offset, arrowHeight, 0);
        }

        StatMode firstMode = statModes.values().iterator().next();
        if (firstMode.getPages().isEmpty()) {
            return baseLocation.clone().add(offset, arrowHeight, 0);
        }

        float yawDeg = firstMode.getPages().get(0).getXRotation();
        double yawRad = Math.toRadians(yawDeg);

        double dx = Math.cos(yawRad) * offset;
        double dz = -Math.sin(yawRad) * offset;

        return baseLocation.clone().add(dx, arrowHeight, dz);
    }

    /**
     * Rotates the entire leaderboard system
     */
    @ApiStatus.Experimental
    public SwitchableLeaderboard rotate(float x) {
        for (StatMode statMode : statModes.values()) {
            for (LeaderboardHologram page : statMode.getPages()) {
                page.rotate(x, 0);
            }
        }

        if (showArrows) {
            leftArrow.setRotation(x, 0).update();
            rightArrow.setRotation(x, 0).update();
        }

        for (StateButton button : stateButtons) {
            if (button.getStateHolograms() != null) {
                for (TextHologram holo : button.getStateHolograms()) {
                    holo.setRotation(x, 0).update();
                }
            }
        }

        return this;
    }

    /**
     * Teleports the leaderboard to a new location
     */
    public SwitchableLeaderboard teleport(Location location) {
        if (!spawned) {
            throw new IllegalStateException("SwitchableLeaderboard must be spawned before teleporting!");
        }

        this.baseLocation = location.clone();

        for (StatMode statMode : statModes.values()) {
            for (LeaderboardHologram page : statMode.getPages()) {
                page.teleport(baseLocation);
            }
        }

        // Teleport arrows
        if (showArrows) {
            Location leftLoc = computeArrowLocation(-arrowOffset);
            Location rightLoc = computeArrowLocation(arrowOffset);

            leftArrow.teleport(leftLoc).update();
            rightArrow.teleport(rightLoc).update();

            if (!singleDirectionCycling) {
                leftInteraction.teleport(leftLoc);
            }
            rightInteraction.teleport(rightLoc);
        }

        // Teleport state buttons
        for (StateButton button : stateButtons) {
            Location buttonLoc = baseLocation.clone().add(
                    button.getRelativePosition().x,
                    button.getRelativePosition().y,
                    button.getRelativePosition().z
            );
            for (TextHologram holo : button.getStateHolograms()) {
                holo.teleport(buttonLoc).update();
            }
            button.getInteraction().teleport(buttonLoc);
        }

        return this;
    }

    /**
     * Updates all pages in all stat modes
     */
    public void updateAll() {
        for (StatMode statMode : statModes.values()) {
            for (LeaderboardHologram page : statMode.getPages()) {
                page.update();
            }
        }
    }

    @Override
    public void onJoin(Player player) {
        if (!playerStates.containsKey(player.getUniqueId())) {
            this.show(player);
        }
    }

    @Override
    public void onQuit(Player player) {
        playerStates.remove(player.getUniqueId());
    }


    public SwitchableLeaderboard setArrowOffset(double arrowOffset) {
        this.arrowOffset = arrowOffset;
        return this;
    }

    public SwitchableLeaderboard setArrowHeight(double arrowHeight) {
        this.arrowHeight = arrowHeight;
        return this;
    }

    public SwitchableLeaderboard setLeftArrowText(String leftArrowText) {
        this.leftArrowText = leftArrowText;
        if (leftArrow != null) {
            leftArrow.setMiniMessageText(leftArrowText).update();
        }
        return this;
    }

    public SwitchableLeaderboard setRightArrowText(String rightArrowText) {
        this.rightArrowText = rightArrowText;
        if (rightArrow != null) {
            rightArrow.setMiniMessageText(rightArrowText).update();
        }
        return this;
    }

    public SwitchableLeaderboard setInteractionBoxSize(Vector3F interactionBoxSize) {
        this.interactionBoxSize = interactionBoxSize;
        if (leftInteraction != null) {
            leftInteraction.setSize(interactionBoxSize);
        }
        if (rightInteraction != null) {
            rightInteraction.setSize(interactionBoxSize);
        }
        return this;
    }

    public SwitchableLeaderboard setNavigationSound(Sound sound, float volume, float pitch) {
        this.navigationSound = sound;
        this.navigationVolume = volume;
        this.navigationPitch = pitch;
        return this;
    }

    public SwitchableLeaderboard setStateChangeSound(Sound sound, float volume, float pitch) {
        this.stateChangeSound = sound;
        this.stateChangeVolume = volume;
        this.stateChangePitch = pitch;
        return this;
    }

    public SwitchableLeaderboard setArrowBackground(int backgroundColor) {
        this.arrowBackground = backgroundColor;
        if (leftArrow != null) {
            leftArrow.setBackgroundColor(backgroundColor).update();
        }
        if (rightArrow != null) {
            rightArrow.setBackgroundColor(backgroundColor).update();
        }
        return this;
    }

    public SwitchableLeaderboard setArrowScale(Vector3F scale) {
        this.arrowScale = scale;
        if (leftArrow != null) {
            leftArrow.setScale(scale.x, scale.y, scale.z).update();
        }
        if (rightArrow != null) {
            rightArrow.setScale(scale.x, scale.y, scale.z).update();
        }
        return this;
    }

    public SwitchableLeaderboard setArrowScale(float x, float y, float z) {
        return setArrowScale(new Vector3F(x, y, z));
    }

    /**
     * Gets the current stat mode for a player
     */
    public String getCurrentStatMode(Player player) {
        PlayerState state = playerStates.get(player.getUniqueId());
        return state != null ? state.getCurrentStatMode() : null;
    }

    /**
     * Gets the current page for a player
     */
    public int getCurrentPage(Player player) {
        PlayerState state = playerStates.get(player.getUniqueId());
        return state != null ? state.getCurrentPage() : 0;
    }

    /**
     * Gets the current state of a button for a player
     */
    public int getButtonState(Player player, String buttonId) {
        PlayerState state = playerStates.get(player.getUniqueId());
        return state != null ? state.getButtonState(buttonId) : 0;
    }

    /**
     * Gets all internal holograms (for registration with HologramManager)
     */
    public List<TextHologram> getAllHolograms() {
        List<TextHologram> holograms = new ArrayList<>();

        for (StatMode statMode : statModes.values()) {
            for (LeaderboardHologram page : statMode.getPages()) {
                holograms.addAll(page.getAllTextHolograms());
            }
        }

        if (showArrows) {
            holograms.add(leftArrow);
            holograms.add(rightArrow);
        }

        for (StateButton button : stateButtons) {
            if (button.getStateHolograms() != null && !button.getStateHolograms().isEmpty()) {
                holograms.addAll(button.getStateHolograms());
            } else {
                holograms.add(button.getHologram());
            }
        }

        return holograms;
    }

    /**
     * Gets all internal interaction boxes
     */
    public List<InteractionBox> getAllInteractionBoxes() {
        List<InteractionBox> interactions = new ArrayList<>();

        if (showArrows) {
            if (!singleDirectionCycling) {
                interactions.add(leftInteraction);
            }
            interactions.add(rightInteraction);
        }

        for (StateButton button : stateButtons) {
            interactions.add(button.getInteraction());
        }

        return interactions;
    }
}