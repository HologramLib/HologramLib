package com.maximde.hologramlib.hologram;


import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemProfile;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.maximde.hologramlib.hook.HeadDatabaseHook;
import com.maximde.hologramlib.utils.PlayerUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.ItemDisplayMeta;
import org.bukkit.Bukkit;
import org.joml.Vector3f;


import java.awt.*;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class ItemHologram extends Hologram<ItemHologram> {

    @Setter
    @Accessors(chain = true)
    protected ItemDisplayMeta.DisplayType displayType = ItemDisplayMeta.DisplayType.FIXED;

    @Setter
    @Accessors(chain = true)
    protected boolean onFire = false;

    @Setter
    @Accessors(chain = true)
    protected ItemStack item = new ItemStack.Builder()
            .type(ItemTypes.IRON_AXE).build();



    public ItemHologram(String id, RenderMode renderMode) {
        super(id, renderMode, EntityTypes.ITEM_DISPLAY);
    }

    public ItemHologram(String id) {
        this(id, RenderMode.ALL);
    }


    @Override
    protected EntityMeta applyMeta() {
        ItemDisplayMeta meta = (ItemDisplayMeta) this.entity.getEntityMeta();
        meta.setInterpolationDelay(-1);
        meta.setTransformationInterpolationDuration(this.interpolationDurationTransformation);
        meta.setPositionRotationInterpolationDuration(this.teleportDuration);
        meta.setTranslation(super.toVector3f(this.translation));
        meta.setLeftRotation(this.leftRotation);
        meta.setRightRotation(this.rightRotation);
        meta.setScale(super.toVector3f(this.scale));
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.valueOf(this.billboard.name()));
        meta.setViewRange((float) this.viewRange);
        meta.setDisplayType(this.displayType);
        meta.setOnFire(this.onFire);
        meta.setItem(this.item);
        meta.setGlowing(this.glowing);
        meta.setInvisible(this.isInvisible);
        if(super.brightness > -1) meta.setBrightnessOverride(super.brightness);
        meta.setGlowColorOverride(this.glowColor);
        return meta;
    }

    /**
     * Sets the item to a player head using the given UUID.
     * The head will automatically glow and be scaled to fit a hologram display.
     * @param uuid The UUID of the player whose skin to use.
     * @return this (for chaining)
     */
    public ItemHologram setPlayerHead(UUID uuid) {
        try {
            List<ItemProfile.Property> properties = new ArrayList<>();
            String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + PlayerUtils.getPlayerSkinUrl(uuid) + "\"}}}";
            String base64Texture = Base64.getEncoder().encodeToString(textureJson.getBytes());

            properties.add(new ItemProfile.Property("textures", base64Texture, null));

            ItemProfile profile = new ItemProfile("PlayerHead", uuid, properties);

            this.item = new ItemStack.Builder()
                    .type(ItemTypes.PLAYER_HEAD)
                    .component(ComponentTypes.PROFILE, profile)
                    .build();

            return this;
        } catch (Exception exception) {
            Bukkit.getLogger().warning("Failed to set player head in ItemHologram: " + exception.getMessage());
            return this;
        }
    }

    /**
     * Sets the item to a player head using a raw base64 texture string.
     * This is useful for custom heads from HeadDatabase or similar plugins.
     * @param base64Texture The base64 encoded texture value (the "Value" field from textures property).
     * @return this (for chaining)
     */
    public ItemHologram setBase64Head(String base64Texture) {
        try {
            List<ItemProfile.Property> properties = new ArrayList<>();
            properties.add(new ItemProfile.Property("textures", base64Texture, null));

            ItemProfile profile = new ItemProfile("CustomHead", UUID.randomUUID(), properties);

            this.item = new ItemStack.Builder()
                    .type(ItemTypes.PLAYER_HEAD)
                    .component(ComponentTypes.PROFILE, profile)
                    .build();

            return this;
        } catch (Exception exception) {
            Bukkit.getLogger().warning("Failed to set base64 head in ItemHologram: " + exception.getMessage());
            return this;
        }
    }

    /**
     * Sets the item to a player head using a skin URL.
     * The URL should point to a Minecraft skin texture.
     * @param skinUrl The URL of the skin texture (e.g., from textures.minecraft.net).
     * @return this (for chaining)
     */
    public ItemHologram setUrlHead(String skinUrl) {
        try {
            List<ItemProfile.Property> properties = new ArrayList<>();
            String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + skinUrl + "\"}}}";
            String base64Texture = Base64.getEncoder().encodeToString(textureJson.getBytes());

            properties.add(new ItemProfile.Property("textures", base64Texture, null));

            ItemProfile profile = new ItemProfile("UrlHead", UUID.randomUUID(), properties);

            this.item = new ItemStack.Builder()
                    .type(ItemTypes.PLAYER_HEAD)
                    .component(ComponentTypes.PROFILE, profile)
                    .build();

            return this;
        } catch (Exception exception) {
            Bukkit.getLogger().warning("Failed to set URL head in ItemHologram: " + exception.getMessage());
            return this;
        }
    }

    /**
     * Sets the item to a player head using a HeadDatabase head ID.
     * Requires HeadDatabase plugin to be installed and enabled.
     * @param headId The HeadDatabase head ID (e.g., "7129").
     * @return this (for chaining)
     */
    public ItemHologram setHeadDatabaseHead(String headId) {
        if (!HeadDatabaseHook.isAvailable()) {
            Bukkit.getLogger().warning("HeadDatabase is not available. Cannot set head with ID: " + headId);
            return this;
        }

        return HeadDatabaseHook.getBase64Texture(headId)
                .map(this::setBase64Head)
                .orElseGet(() -> {
                    Bukkit.getLogger().warning("Could not find HeadDatabase head with ID: " + headId);
                    return this;
                });
    }

    @Override
    protected ItemHologram copy() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000);
        return this.copy(this.id + "_copy_" + randomNumber);
    }

    @Override
    protected ItemHologram copy(String id) {
        ItemHologram copy = new ItemHologram(id, this.renderMode);
        copy.item = this.item;
        copy.glowColor = this.glowColor;
        copy.glowing = this.glowing;
        copy.onFire = this.onFire;
        copy.displayType = this.displayType;
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
        copy.updateTaskPeriod = this.updateTaskPeriod;
        copy.maxPlayerRenderDistanceSquared = this.maxPlayerRenderDistanceSquared;
        return copy;
    }
}