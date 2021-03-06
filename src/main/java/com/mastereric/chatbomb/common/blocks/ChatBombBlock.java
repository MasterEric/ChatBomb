package com.mastereric.chatbomb.common.blocks;

import com.mastereric.chatbomb.ChatBomb;
import com.mastereric.chatbomb.common.entity.PrimedChatBombEntity;
import com.mastereric.chatbomb.util.LogUtility;
import net.fabricmc.fabric.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.TranslatableTextComponent;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

import javax.annotation.Nullable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatBombBlock extends Block {
    // UNSTABLE property on TNT allows it to be ignited with a punch.
    public static final BooleanProperty UNSTABLE = Properties.UNSTABLE;

    // If a message within radius matches this Regex string, ignite the Chat Bomb.
    private static String REGEX = ".*" +
            "(boom)|(blast)|(explode)|(explosive)|(dynamite)|" +
            "(tnt)|(detonate)|(bomb)|(burst)|(bang)" +
            ".*";
    private static Pattern REGEX_PATTERN = Pattern.compile(REGEX);

    public static final int RADIUS = 8;

    public static final boolean VERBOSE = true;

    public ChatBombBlock() {
        super(FabricBlockSettings.copy(Blocks.TNT).build());
    }

    /*
      Chat Bomb default detonation logic.
     */

    public void primeTnt(World world, BlockPos blockPos, @Nullable LivingEntity primer, boolean fast) {
        if (!world.isClient()) {
            LogUtility.debug("Priming Chat Bomb on server with igniter '%s', position (%s, %s, %s)",
                    (primer == null ? "null" : primer.getDisplayName().getFormattedText()), blockPos.getX(), blockPos.getY(), blockPos.getZ());
            if (primer instanceof PlayerEntity) {
                grantTriggerAdvancement((PlayerEntity) primer);
            }
            PrimedChatBombEntity primedChatBombEntity = new PrimedChatBombEntity(world,
                    (double)((float)blockPos.getX() + 0.5F), (double)blockPos.getY(),
                    (double)((float)blockPos.getZ() + 0.5F), primer);
            if (fast) {
                // TNT destroyed by explosions has a shortened fuse.
                primedChatBombEntity.setFuse((short)(world.random.nextInt(primedChatBombEntity.getFuseTimer() / 4)
                        + primedChatBombEntity.getFuseTimer() / 8));
            }
            LogUtility.debug("Spawning Chat Bomb entity (id '%d', uuid '%s')",
                    primedChatBombEntity.getEntityId(), primedChatBombEntity.getUuidAsString());
            world.spawnEntity(primedChatBombEntity);
            world.playSound(null, primedChatBombEntity.x, primedChatBombEntity.y, primedChatBombEntity.z,
                    SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCK, 1.0F, 1.0F);
        }
    }

    /**
     * Ignite the Chat Bomb when it receives Redstone power.
     * @param state
     * @param world
     * @param pos
     * @param oldState
     */
    @Override
    public void onBlockAdded(final BlockState state, final World world, final BlockPos pos, final BlockState oldState) {
        if (oldState.getBlock() == state.getBlock()) {
            return;
        }
        if (world.isReceivingRedstonePower(pos)) {
            this.primeTnt(world, pos, null, false);
            world.clearBlockState(pos);
        }
    }

    /**
     * Ignite the Chat Bomb when it receives Redstone power.
     * @param state
     * @param world
     * @param pos
     * @param block
     * @param neighborPos
     */
    @Override
    public void neighborUpdate(final BlockState state, final World world, final BlockPos pos, final Block block,
                               final BlockPos neighborPos) {
        if (world.isReceivingRedstonePower(pos)) {
            this.primeTnt(world, pos, null, false);
            world.clearBlockState(pos);
        }
    }

    /**
     * Ignite the Chat Bomb when it breaks, if it is unstable.
     * @param world
     * @param pos
     * @param state
     * @param player
     */
    @Override
    public void onBreak(final World world, final BlockPos pos, final BlockState state, final PlayerEntity player) {
        if (!world.isClient() && !player.isCreative() && state.<Boolean>get(ChatBombBlock.UNSTABLE)) {
            this.primeTnt(world, pos, null, false);
        }
        super.onBreak(world, pos, state, player);
    }

    /**
     * Ignite the Chat Bomb if another explosion destroys it.
     * @param world
     * @param pos
     * @param explosion
     */
    @Override
    public void onDestroyedByExplosion(final World world, final BlockPos pos, final Explosion explosion) {
        if (world.isClient()) {
            return;
        }
        this.primeTnt(world, pos, explosion.getCausingEntity(), true);
    }

    /**
     * Ignite the Chat Bomb if it is clicked with Flint and Steel or Fire Charge.
     * @param state
     * @param world
     * @param pos
     * @param player
     * @param hand
     * @param facing
     * @param hitX
     * @param hitY
     * @param vFloat10
     * @return
     */
    @Override
    public boolean activate(final BlockState state, final World world, final BlockPos pos, final PlayerEntity player,
                            final Hand hand, final Direction facing, final float hitX, final float hitY,
                            final float vFloat10) {
        final ItemStack vItemStack11 = player.getStackInHand(hand);
        final Item vItem12 = vItemStack11.getItem();
        if (vItem12 == Items.FLINT_AND_STEEL || vItem12 == Items.FIRE_CHARGE) {
            this.primeTnt(world, pos, player, false);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 11);
            if (vItem12 == Items.FLINT_AND_STEEL) {
                vItemStack11.applyDamage(1, player);
            } else {
                vItemStack11.subtractAmount(1);
            }
            return true;
        }
        return super.activate(state, world, pos, player, hand, facing, hitX, hitY, vFloat10);
    }

    @Override
    public void onEntityCollision(final BlockState state, final World world, final BlockPos pos, final Entity entity) {
        if (!world.isClient() && entity instanceof ProjectileEntity) {
            final ProjectileEntity projectileEntity = (ProjectileEntity)entity;
            final Entity igniter = projectileEntity.getOwner();
            if (projectileEntity.isOnFire()) {
                this.primeTnt(world, pos, (igniter instanceof LivingEntity) ? ((LivingEntity)igniter) : null, false);
                world.clearBlockState(pos);
            }
        }
    }

    /*
      Chat Bomb parsing and chat detonation logic.
     */

    /**
     * Evaluate a given chat message.
     * @param player
     * @param chatMessage
     */
    public static void evaluateChatMessage(PlayerEntity player, String chatMessage) {
        if (chatMessage.startsWith("/"))
            return; // Don't trigger for commands.

        Matcher m = REGEX_PATTERN.matcher(chatMessage.toLowerCase());
        if (m.find()) {
            String triggerWord;
            if (m.group(1) != null)
                triggerWord = m.group(1);
            else
                triggerWord = m.group(0);
            LogUtility.info("Chat Bomb trigger word detected: %s", triggerWord);
            detonateChatBombs(player.getEntityWorld(), new BlockPos(player.getPos()), RADIUS, player, triggerWord);
        }
    }

    /**
     * Detonate chatbombs within radius blocks of the center.
     * @param world
     * @param center
     * @param radius
     */
    public static void detonateChatBombs(World world, BlockPos center, int radius, PlayerEntity igniter,
                                         String triggerWord) {
        BlockPos corner1 = center.add(-radius, -radius, -radius);
        BlockPos corner2 = center.add(radius, radius, radius);
        detonateChatBombsWithinAABB(world, corner1, corner2, igniter, triggerWord);
    }

    /**
     * Detonate chatbombs within radius blocks of the center.
     * @param world
     * @param corner1
     * @param corner2
     * @param triggerWord
     */
    public static void detonateChatBombsWithinAABB(World world, BlockPos corner1, BlockPos corner2,
                                                   PlayerEntity igniter, String triggerWord) {
        LogUtility.debug("Detecting Chat Bombs within AABB: (%d, %d, %d) -> (%d, %d, %d)",
                corner1.getX(), corner1.getY(), corner1.getZ(), corner2.getX(), corner2.getY(), corner2.getZ());
        for (BlockPos blockPos : BlockPos.iterateBoxPositions(corner1, corner2)) {
            if (world.getBlockState(blockPos).getBlock() == ChatBomb.Blocks.CHAT_BOMB) {
                LogUtility.debug("Triggering explosion by igniter '%s' with trigger word '%s', at position (%d, %d, %d)",
                        igniter.getDisplayName().getFormattedText(), triggerWord, corner1.getX(), corner1.getY(), corner1.getZ());
                announceExplosion(igniter, triggerWord);
                ((ChatBombBlock) ChatBomb.Blocks.CHAT_BOMB).primeTnt(world, blockPos, igniter, false);
                world.clearBlockState(blockPos);
            }
        }
    }

    /**
     * Send a status message to the igniter.
     * @param igniter
     * @param triggerWord
     */
    public static void announceExplosion(PlayerEntity igniter, String triggerWord) {

        if (VERBOSE) {
            igniter.addChatMessage(new TranslatableTextComponent("block.chatbomb.chatbomb.trigger", triggerWord),
                    false);
        }
    }

    public static void grantTriggerAdvancement(PlayerEntity igniter) {
    }
}
