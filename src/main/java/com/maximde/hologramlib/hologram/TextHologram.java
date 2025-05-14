package com.maximde.hologramlib.hologram;

import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.maximde.hologramlib.HologramLib;
import com.maximde.hologramlib.utils.MiniMessage;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.TextDisplay;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class TextHologram extends Hologram<TextHologram> {

    protected Component text = Component.text("");

    @Setter @Getter @Accessors(chain = true)
    private boolean shadow = true;

    @Setter @Getter @Accessors(chain = true)
    private int maxLineWidth = 200;

    /**
     * The background color is an argb integer which can be generated <a href="https://argb-int-calculator.netlify.app/">here</a>
     */
    @Setter @Getter @Accessors(chain = true)
    private int backgroundColor;

    /**
     * Controls how the text appears through blocks.
     * When true, text will be visible through solid blocks.
     * When false, blocks will occlude the text normally.
     */
    @Setter @Getter @Accessors(chain = true)
    private boolean seeThroughBlocks = false;

    @Setter @Getter @Accessors(chain = true)
    private TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.CENTER;

    @Setter @Getter @Accessors(chain = true)
    private byte textOpacity = (byte) -1;

    /**
     * Creates a new text hologram with the specified ID and render mode.
     *
     * @param id Unique identifier for this hologram. Cannot contain spaces.
     * @param renderMode Determines how and to whom the hologram is rendered
     * @throws IllegalArgumentException if id contains spaces
     */
    public TextHologram(String id, RenderMode renderMode) {
        super(id, renderMode, EntityTypes.TEXT_DISPLAY);
    }

    /**
     * Creates a new text hologram with the specified ID and nearby render mode.
     *
     * @param id Unique identifier for this hologram. Cannot contain spaces.
     * @throws IllegalArgumentException if id contains spaces
     */
    public TextHologram(String id) {
        this(id, RenderMode.ALL);
    }

    /**
     * Creates a copy of this hologram with a new ID.
     * The new ID will be the original ID with '_copy_' followed by a random number appended.
     * @return A new TextHologram instance with copied properties
     */
    @Override
    protected TextHologram copy() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000);
        return this.copy(this.id + "_copy_" + randomNumber);
    }

    /**
     * Creates a copy of this hologram with a new ID.
     *
     * @return A new TextHologram instance with copied properties
     */
    @Override
    public TextHologram copy(String id) {
        TextHologram copy = new TextHologram(id, this.renderMode);
        copy.text = this.text;
        copy.scale = new Vector3f(this.scale);
        copy.translation = new Vector3f(this.translation);
        copy.rightRotation = new Quaternion4f(this.rightRotation.getX(), this.rightRotation.getY(),
                this.rightRotation.getZ(), this.rightRotation.getW());
        copy.leftRotation = new Quaternion4f(this.leftRotation.getX(), this.leftRotation.getY(),
                this.leftRotation.getZ(), this.leftRotation.getW());
        copy.billboard = this.billboard;
        copy.teleportDuration = this.teleportDuration;
        copy.interpolationDurationTransformation = this.interpolationDurationTransformation;
        copy.viewRange = this.viewRange;
        copy.shadow = this.shadow;
        copy.maxLineWidth = this.maxLineWidth;
        copy.backgroundColor = this.backgroundColor;
        copy.seeThroughBlocks = this.seeThroughBlocks;
        copy.alignment = this.alignment;
        copy.textOpacity = this.textOpacity;
        copy.updateTaskPeriod = this.updateTaskPeriod;
        copy.maxPlayerRenderDistanceSquared = this.maxPlayerRenderDistanceSquared;
        return copy;
    }


    @Override
    protected EntityMeta applyMeta() {
        TextDisplayMeta meta = (TextDisplayMeta) super.entity.getEntityMeta();
        meta.setText(getTextAsComponent());
        meta.setInterpolationDelay(-1);
        meta.setTransformationInterpolationDuration(this.interpolationDurationTransformation);
        meta.setPositionRotationInterpolationDuration(this.teleportDuration);
        meta.setTranslation(super.toVector3f(this.translation));
        meta.setLeftRotation(this.leftRotation);
        meta.setRightRotation(this.rightRotation);
        meta.setScale(super.toVector3f(this.scale));
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.valueOf(this.billboard.name()));
        meta.setLineWidth(this.maxLineWidth);
        meta.setViewRange((float) this.viewRange);
        meta.setBackgroundColor(this.backgroundColor);
        meta.setTextOpacity(this.textOpacity);
        meta.setShadow(this.shadow);
        if(super.brightness > -1) meta.setBrightnessOverride(super.brightness);
        meta.setSeeThrough(this.seeThroughBlocks);
        setInternalAlignment(meta);
        return meta;
    }

    private void setInternalAlignment(TextDisplayMeta meta) {
        switch (this.alignment) {
            case LEFT -> meta.setAlignLeft(true);
            case RIGHT -> meta.setAlignRight(true);
        }
    }

    public Component getTextAsComponent() {
        return this.text;
    }

    public String getText() {
        return ((TextComponent) this.text).content();
    }

    public String getTextWithoutColor() {
        return ChatColor.stripColor(getText());
    }

    public TextHologram setText(String text) {
        this.text = Component.text(replaceFontImages(text));
        return this;
    }

    public TextHologram setText(Component component) {
        this.text = component;
        return this;
    }

    public TextHologram setMiniMessageText(String text) {
        this.text = MiniMessage.get(replaceFontImages(text));
        return this;
    }

    private String replaceFontImages(String string) {
        return HologramLib.getReplaceText().replace(string);
    }
}
