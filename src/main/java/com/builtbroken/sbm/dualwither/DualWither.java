package com.builtbroken.sbm.dualwither;

import net.minecraft.entity.boss.EntityWither;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashSet;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 10/18/2018.
 */
@Mod(modid = DualWither.DOMAIN, acceptableRemoteVersions = "*")
@Mod.EventBusSubscriber()
public class DualWither
{
    public static final String DOMAIN = "sbmdualwither";
    public static final String PREFIX = DOMAIN + ":";

    public static final String NBT_SPAWN_CHECK = PREFIX + "spawned";
    public static final String NBT_SPAWN_HOST = PREFIX + "host";

    private static final HashSet<EntityWither> ignoreList = new HashSet();

    @SubscribeEvent
    public static void onEntitySpawned(LivingSpawnEvent event)
    {
        //Track withers spawned by mob spawners
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityWither)
        {
            ignoreList.add((EntityWither) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onEntityJoinWorld(EntityJoinWorldEvent event)
    {
        if (!event.getWorld().isRemote && event.getEntity() instanceof EntityWither)
        {
            //Ignore mob spawner entities
            if (ignoreList.contains(event.getEntity()))
            {
                ignoreList.remove(event.getEntity());
                return;
            }

            EntityWither wither = (EntityWither) event.getEntity();

            //Only trigger on newly spawned withers
            if (!wither.getEntityData().hasKey(NBT_SPAWN_CHECK) && wither.ticksExisted < 1)
            {
                wither.getEntityData().setString(NBT_SPAWN_CHECK, "main");
                spawnSecondaryWithers(wither);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityHurt(LivingHurtEvent event)
    {
        //Block damage caused by other withers
        if (event.getEntity() instanceof EntityWither && shouldBlockDamage((EntityWither) event.getEntity(), event.getSource()))
        {
            if (event.getSource().getImmediateSource() instanceof EntityWither)
            {
                ((EntityWither) event.getSource().getImmediateSource()).setAttackTarget(null);
            }
            else if (event.getSource().getTrueSource() instanceof EntityWither)
            {
                ((EntityWither) event.getSource().getTrueSource()).setAttackTarget(null);
            }
            if (event.isCancelable())
            {
                event.setCanceled(true);
            }
        }
    }

    protected static boolean shouldBlockDamage(EntityWither wither, DamageSource source)
    {
        if (source.getImmediateSource() instanceof EntityWither)
        {
            //allow self harm
            return source.getImmediateSource() != wither;
        }
        else if (source.getTrueSource() instanceof EntityWither)
        {
            //allow self harm
            return source.getTrueSource() != wither;
        }
        return false;
    }


    protected static void spawnSecondaryWithers(EntityWither mainWither)
    {
        for (EntityPlayerMP entityplayermp : mainWither.world.getEntitiesWithinAABB(EntityPlayerMP.class, mainWither.getEntityBoundingBox().grow(50.0D)))
        {
            spawnWithOnPlayer(entityplayermp, mainWither);
        }
    }

    protected static void spawnWithOnPlayer(EntityPlayerMP player, EntityWither mainWither)
    {
        EntityWither wither = new EntityWither(player.world);
        wither.getEntityData().setString(NBT_SPAWN_CHECK, "secondary");
        wither.getEntityData().setInteger(NBT_SPAWN_HOST, mainWither.getEntityId());

        if (placeWither(player, wither))
        {
            spawnWither(wither);
        }
    }

    protected static boolean placeWither(EntityPlayerMP player, EntityWither wither)
    {
        int i = MathHelper.floor(player.posX) - 5; //TODO place behind player
        int j = MathHelper.floor(player.posZ) - 5;
        int k = MathHelper.floor(player.getEntityBoundingBox().minY);

        for (int offsetX = 0; offsetX <= 10; ++offsetX)
        {
            for (int offsetZ = 0; offsetZ <= 10; ++offsetZ)
            {
                if (canPlaceWitherHere(player.world, i, j, k, offsetX, offsetZ))
                {
                    wither.setLocationAndAngles((double) ((float) (i + offsetX) + 0.5F), (double) k, (double) ((float) (j + offsetZ) + 0.5F), wither.rotationYaw, wither.rotationPitch);
                    wither.getNavigator().clearPath();
                    return true;
                }
            }
        }
        return false;
    }

    protected static boolean canPlaceWitherHere(World world, int x, int z, int y, int xOffset, int zOffset)
    {
        BlockPos blockpos = new BlockPos(x + xOffset, y, z + zOffset);
        return world.isAirBlock(blockpos) && world.isAirBlock(blockpos.up()) && world.isAirBlock(blockpos.up(2));
    }

    protected static void spawnWither(EntityWither wither)
    {
        wither.world.spawnEntity(wither);
        //TODO set HP and other values to make wither weaker
    }

}
