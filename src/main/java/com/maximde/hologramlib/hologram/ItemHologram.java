package com.maximde.hologramlib.hologram;


import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.ItemDisplayMeta;
import me.tofaa.entitylib.meta.display.TextDisplayMeta;
import org.bukkit.entity.Player;
import org.joml.Vector3f;


import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;


@Getter
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
            .type(ItemTypes.AIR).build();

    @Setter
    @Accessors(chain = true)
    protected boolean glowing = false;

    /**
     * Only visible if 'ItemHologram#glowing' is set to true
     */
    @Accessors(chain = true)
    protected int glowColor = 0;

    public interface ItemModifier { ItemDisplayMeta onSend(Player player, ItemDisplayMeta itemDisplayMeta); }


    public ItemHologram(String id, RenderMode renderMode, ItemModifier modifier) {
        super(id, renderMode, EntityTypes.ITEM_DISPLAY, new BaseMetaSender() {
            @Override
            public ItemDisplayMeta itemDisplay(Player player, ItemDisplayMeta itemDisplayMeta) {
                return modifier.onSend(player, itemDisplayMeta);
            }
        });
    }

    public ItemHologram(String id, RenderMode renderMode) {
        super(id, renderMode, EntityTypes.ITEM_DISPLAY);
    }

    public ItemHologram(String id) {
        this(id, RenderMode.NEARBY);
    }

    /**
     * Sets the RGB color for the item's glow effect. (The color can be wrong if server version is below 1.20.5)
     * Only applies when glowing is set to true.
     */
    public ItemHologram setGlowColor(Color color) {
        int rgb = color.getRGB();
        this.glowColor = ((rgb & 0xFF0000) >> 16) |
                (rgb & 0x00FF00) |
                ((rgb & 0x0000FF) << 16);
        return this;
    }


    @Override
    protected EntityMeta createMeta() {
        ItemDisplayMeta meta = (ItemDisplayMeta) EntityMeta.createMeta(super.entityID, EntityTypes.ITEM_DISPLAY);
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
        meta.setGlowColorOverride(this.glowColor);
        return meta;
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
        copy.nearbyEntityScanningDistance = this.nearbyEntityScanningDistance;
        return copy;
    }
}