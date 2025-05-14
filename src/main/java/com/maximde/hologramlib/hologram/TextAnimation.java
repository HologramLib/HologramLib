package com.maximde.hologramlib.hologram;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TextAnimation {

    private final List<String> textFrames = new ArrayList<>();

    /**
     * The display will be updated every x ticks
     * 20 ticks = 1 second
     */
    private @Setter @Accessors(chain = true) long speed = 20;

    /**
     * Delay in ticks when the animation should start
     * 20 ticks = 1 second
     */
    private @Setter @Accessors(chain = true) long delay = 20;


    public TextAnimation() {}

    public TextAnimation(int speed) {
        this.speed = speed;
    }

    public TextAnimation(int speed, int delay) {
        this.speed = speed;
        this.delay = delay;
    }

    public TextAnimation addFrame(String text) {
        this.textFrames.add(text);
        return this;
    }

    public TextAnimation removeFrame(int number) {
        this.textFrames.remove(number);
        return this;
    }

    public TextAnimation removeLastFrame() {
        removeFrame(this.textFrames.size() - 1);
        return this;
    }

    public TextAnimation removeFirstFrame() {
        removeFrame(0);
        return this;
    }

    public TextAnimation clearFrames() {
        this.textFrames.clear();
        return this;
    }

}
