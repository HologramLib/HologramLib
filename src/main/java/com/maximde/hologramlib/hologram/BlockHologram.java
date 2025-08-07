package com.maximde.hologramlib.hologram;

import com.github.retrooper.packetevents.protocol.entity.type.EntityType;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
import com.github.retrooper.packetevents.util.Quaternion4f;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import me.tofaa.entitylib.meta.EntityMeta;
import me.tofaa.entitylib.meta.display.AbstractDisplayMeta;
import me.tofaa.entitylib.meta.display.BlockDisplayMeta;
import org.joml.Vector3f;

import java.util.concurrent.ThreadLocalRandom;


@Getter
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class BlockHologram extends Hologram<BlockHologram> {

    @Setter
    @Accessors(chain = true)
    protected int block = 0;

    @Setter
    @Accessors(chain = true)
    protected boolean onFire = false;

    public BlockHologram(String id) {
        this(id, RenderMode.ALL);
    }

    public BlockHologram(String id, RenderMode renderMode) {
        super(id, renderMode, EntityTypes.BLOCK_DISPLAY);
    }

    public BlockHologram(String id, EntityType entityType) {
        super(id, entityType);
    }

    @Override
    protected EntityMeta applyMeta() {
        BlockDisplayMeta meta = (BlockDisplayMeta) this.entity.getEntityMeta();
        meta.setInterpolationDelay(-1);
        meta.setTransformationInterpolationDuration(this.interpolationDurationTransformation);
        meta.setPositionRotationInterpolationDuration(this.teleportDuration);
        meta.setTranslation(super.toVector3f(this.translation));
        meta.setScale(super.toVector3f(this.scale));
        meta.setBillboardConstraints(AbstractDisplayMeta.BillboardConstraints.valueOf(this.billboard.name()));
        meta.setViewRange((float) this.viewRange);
        meta.setBlockId(this.block);
        meta.setLeftRotation(this.leftRotation);
        meta.setRightRotation(this.rightRotation);
        meta.setOnFire(this.onFire);
        meta.setInvisible(this.isInvisible);
        if(super.brightness > -1) meta.setBrightnessOverride(super.brightness);
        meta.setGlowing(this.glowing);
        meta.setGlowColorOverride(this.glowColor);
        return meta;
    }


    protected BlockHologram copy() {
        int randomNumber = ThreadLocalRandom.current().nextInt(100000);
        return this.copy(this.id + "_copy_" + randomNumber);
    }

    @Override
    protected BlockHologram copy(String id) {
        BlockHologram copy = new BlockHologram(id, this.renderMode);
        copy.block = this.block;
        copy.glowColor = this.glowColor;
        copy.glowing = this.glowing;
        copy.onFire = this.onFire;
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