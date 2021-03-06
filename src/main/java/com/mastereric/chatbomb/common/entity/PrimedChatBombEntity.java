package com.mastereric.chatbomb.common.entity;

import com.mastereric.chatbomb.ChatBomb;
import com.mastereric.chatbomb.util.LogUtility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class PrimedChatBombEntity extends Entity {
    private static final TrackedData<Integer> FUSE;
    @Nullable
    private LivingEntity causingEntity;
    private int fuseTimer;

    public PrimedChatBombEntity(World var1) {
        super(ChatBomb.Entities.CHAT_BOMB, var1);
        this.fuseTimer = 80;
        this.field_6033 = true;
        this.fireImmune = true;
        this.setSize(0.98F, 0.98F);
    }

    public PrimedChatBombEntity(World world, double xPos, double yPos, double zPos, @Nullable LivingEntity igniter) {
        this(world);
        LogUtility.debug("Chat Bomb entity instantiated at pos (%f, %f, %s) with igniter '%s'",
                xPos, yPos, zPos, (igniter == null ? "null" : igniter.getDisplayName().getFormattedText()));
        this.setPosition(xPos, yPos, zPos);
        float var9 = (float)(Math.random() * 6.2831854820251465D);
        this.velocityX = (double)(-((float)Math.sin((double)var9)) * 0.02F);
        this.velocityY = 0.20000000298023224D;
        this.velocityZ = (double)(-((float)Math.cos((double)var9)) * 0.02F);
        this.setFuse(80);
        this.prevX = xPos;
        this.prevY = yPos;
        this.prevZ = zPos;
        this.causingEntity = igniter;
    }

    protected void initDataTracker() {
        this.dataTracker.startTracking(FUSE, 80);
    }

    protected boolean method_5658() {
        return false;
    }

    public boolean doesCollide() {
        return !this.invalid;
    }

    public void update() {
        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;
        if (!this.isUnaffectedByGravity()) {
            this.velocityY -= 0.03999999910593033D;
        }

        this.move(MovementType.SELF, this.velocityX, this.velocityY, this.velocityZ);
        this.velocityX *= 0.9800000190734863D;
        this.velocityY *= 0.9800000190734863D;
        this.velocityZ *= 0.9800000190734863D;
        if (this.onGround) {
            this.velocityX *= 0.699999988079071D;
            this.velocityZ *= 0.699999988079071D;
            this.velocityY *= -0.5D;
        }

        --this.fuseTimer;
        if (this.fuseTimer <= 0) {
            LogUtility.debug("Chat Bomb fuse timer expired at age %d, pos (%f, %f, %f)",
                    this.age, this.x, this.y, this.z);
            this.invalidate();
            if (!this.world.isClient) {
                this.explode();
            }
        } else {
            this.method_5713();
            this.world.addParticle(ParticleTypes.SMOKE, this.x, this.y + 0.5D, this.z, 0.0D, 0.0D, 0.0D);
        }

    }

    private void explode() {
        this.world.createExplosion(this, ChatBomb.CHATBOMB_DAMAGE, this.x, this.y + (double)(this.height / 16.0F), this.z, 4.0F, false, true);
    }

    protected void writeCustomDataToTag(CompoundTag var1) {
        var1.putShort("Fuse", (short)this.getFuseTimer());
    }

    protected void readCustomDataFromTag(CompoundTag var1) {
        this.setFuse(var1.getShort("Fuse"));
    }

    @Nullable
    public LivingEntity getCausingEntity() {
        return this.causingEntity;
    }

    public float getEyeHeight() {
        return 0.0F;
    }

    public void setFuse(int var1) {
        this.dataTracker.set(FUSE, var1);
        this.fuseTimer = var1;
    }

    public void onTrackedDataSet(TrackedData<?> var1) {
        if (FUSE.equals(var1)) {
            this.fuseTimer = this.getFuse();
        }

    }

    public int getFuse() {
        return this.dataTracker.get(FUSE);
    }

    public int getFuseTimer() {
        return this.fuseTimer;
    }

    static {
        FUSE = DataTracker.registerData(PrimedChatBombEntity.class, TrackedDataHandlerRegistry.INTEGER);
    }
}
