package com.builtbroken.sbm.dualwither;

import java.util.HashSet;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/18/2018.
 */
@Mod(DualWither.DOMAIN)
@Mod.EventBusSubscriber
public class DualWither
{
    public static final String DOMAIN = "sbmdualwither";
    public static final String PREFIX = DOMAIN + ":";

    public static final String NBT_SPAWN_CHECK = PREFIX + "spawned";
    public static final String NBT_SPAWN_HOST = PREFIX + "host";

    private static final HashSet<WitherEntity> ignoreList = new HashSet<>();

    @SubscribeEvent
    public static void onEntitySpawned(LivingSpawnEvent event)
    {
        //Track withers spawned by mob spawners
        if (!event.getWorld().isRemote() && event.getEntity() instanceof WitherEntity)
        {
            ignoreList.add((WitherEntity) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (!event.getWorld().isRemote() && event.getEntity() instanceof WitherEntity)
        {
            //Ignore mob spawner entities
            if (ignoreList.contains(event.getEntity()))
            {
                ignoreList.remove(event.getEntity());
                return;
            }

            WitherEntity wither = (WitherEntity) event.getEntity();

            //Only trigger on newly spawned withers
            if (!wither.getEntityData().contains(NBT_SPAWN_CHECK) && wither.ticksExisted < 1)
            {
                wither.getEntityData().putString(NBT_SPAWN_CHECK, "main");
                spawnSecondaryWithers(wither);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event)
    {
        //Block damage caused by other withers
        if (event.getEntity() instanceof WitherEntity && shouldBlockDamage((WitherEntity) event.getEntity(), event.getSource()))
        {
            if (event.getSource().getImmediateSource() instanceof WitherEntity)
            {
                ((WitherEntity) event.getSource().getImmediateSource()).setAttackTarget(null);
            }
            else if (event.getSource().getTrueSource() instanceof WitherEntity)
            {
                ((WitherEntity) event.getSource().getTrueSource()).setAttackTarget(null);
            }
            if (event.isCancelable())
            {
                event.setCanceled(true);
            }
        }
    }

    protected static boolean shouldBlockDamage(WitherEntity wither, DamageSource source)
    {
        if (source.getImmediateSource() instanceof WitherEntity)
        {
            //allow self harm
            return source.getImmediateSource() != wither;
        }
        else if (source.getTrueSource() instanceof WitherEntity)
        {
            //allow self harm
            return source.getTrueSource() != wither;
        }
        return false;
    }


    protected static void spawnSecondaryWithers(WitherEntity mainWither)
    {
        for (ServerPlayerEntity entityplayermp : mainWither.world.getEntitiesWithinAABB(ServerPlayerEntity.class, mainWither.getBoundingBox().grow(50.0D)))
        {
            spawnWithOnPlayer(entityplayermp, mainWither);
        }
    }

    protected static void spawnWithOnPlayer(ServerPlayerEntity player, WitherEntity mainWither)
    {
        WitherEntity wither = new WitherEntity(EntityType.WITHER, player.world);
        wither.getEntityData().putString(NBT_SPAWN_CHECK, "secondary");
        wither.getEntityData().putInt(NBT_SPAWN_HOST, mainWither.getEntityId());

        if (placeWither(player, wither)) //TODO randomize to prevent 50+ withers from spawning at once
        {
            spawnWither(wither);
            player.sendStatusMessage(new StringTextComponent("\u00A7cBehind you"), true);
        }
    }

    protected static boolean placeWither(ServerPlayerEntity player, WitherEntity wither)
    {
        Vec3d offset = new Vec3d(0, 0, -1D);
        offset = offset.rotateYaw(-player.rotationYawHead * 0.017453292F);

        int x = (int)Math.floor(player.posX);
        int y = (int)Math.floor(player.posY) + 1;
        int z = (int)Math.floor(player.posZ);

        for (int d = 10; d > 0; d--)
        {
            for (int h = -2; h<=4; h++)
            {
                Vec3d placeOffset = offset.scale(d);
                if (canPlaceWitherHere(player.world, x, y + h, z, (int) placeOffset.x, (int) placeOffset.z))
                {
                    placeOffset = placeOffset.add(player.posX, player.posY + h, player.posZ);
                    wither.setLocationAndAngles(placeOffset.x, placeOffset.y, placeOffset.z, player.rotationYawHead, 0);
                    return true;
                }
            }
        }
        return false;
    }

    protected static boolean canPlaceWitherHere(World world, int x, int z, int y, int xOffset, int zOffset)
    {
        //TODO raytrace to ensure wither is still in line of sight
        BlockPos blockpos = new BlockPos(x + xOffset, y, z + zOffset);
        return world.isAirBlock(blockpos) && world.isAirBlock(blockpos.up()) && world.isAirBlock(blockpos.up(2));
    }

    protected static void spawnWither(WitherEntity wither)
    {
        wither.world.addEntity(wither);
        //TODO set HP and other values to make wither weaker
    }

}
