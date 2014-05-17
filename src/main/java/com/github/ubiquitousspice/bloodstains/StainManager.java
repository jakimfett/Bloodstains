package com.github.ubiquitousspice.bloodstains;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import lzma.sdk.lzma.Decoder;
import lzma.streams.LzmaInputStream;
import lzma.streams.LzmaOutputStream;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.ubiquitousspice.bloodstains.data.BloodStain;
import com.github.ubiquitousspice.bloodstains.data.PlayerState;
import com.github.ubiquitousspice.bloodstains.data.PlayerStateContainer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class StainManager
{
    private static StainManager                       INSTANCE   = new StainManager();
    private final HashMap<UUID, PlayerStateContainer> containers = Maps.newHashMap();
    private final List<BloodStain>                    stains     = Lists.newLinkedList();
    
    private static final String FILE_NAME = "BloodStains.dat";
    private static final Logger LOGGER = LogManager.getLogger();

    public static void init()
    {
        INSTANCE = new StainManager();
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        FMLCommonHandler.instance().bus().register(INSTANCE);
    }

    public static Collection<BloodStain> getStains()
    {
        return INSTANCE.stains;
    }

    @SubscribeEvent
    public void playerTick(final PlayerTickEvent e)
    {
        final PlayerState state = new PlayerState(e.player);

        PlayerStateContainer container = containers.get(e.player.getUniqueID());
        if (container == null)
        {
            container = new PlayerStateContainer(e.player.getUniqueID(), e.player.getDisplayName(), state);
            containers.put(e.player.getUniqueID(), container);
            LogManager.getLogger().trace("Creating container for {}", e.player.getDisplayName());
        }
        else
        {
            container.addState(state);
        }
    }

    @SubscribeEvent
    public void playerDeath(LivingDeathEvent e)
    {
        if (!(e.entity instanceof EntityPlayer))
        {
            return;
        }

        // get container.
        PlayerStateContainer container = containers.get(e.entity.getUniqueID());
        if (container == null)
        {
            return;
        }

        // make stain, and clean.
        BloodStain stain = container.getBloodStain();
        stains.add(stain);

        LogManager.getLogger().debug("Adding stain for {} at {}, {}, {}", stain.username, stain.x, stain.y, stain.z);
    }

    @SubscribeEvent
    public void worldLoad(WorldEvent.Load e) throws FileNotFoundException, IOException
    {
        if (e.world.isRemote)
        {
            // ignore client stuff
            return;
        }
        
        File inFile = getFile(e.world);
        LOGGER.debug("Reading from file {}", inFile);
        
        if (!inFile.exists())
        {
            LOGGER.debug("File {} does not exist, skipping...", inFile);
            return;
        }
        
        DataInputStream stream = new DataInputStream(new LzmaInputStream(new FileInputStream(inFile), new Decoder()));
        
        int size = stream.readInt();
        for (int i = 0; i < size; i++)
            stains.add(BloodStain.readFrom(stream));
        
        stream.close();
        
        LOGGER.info("Reading {} BloodStains in world '{}'", size, e.world.provider.getDimensionName());
    }

    @SubscribeEvent
    public void worldSave(WorldEvent.Save e) throws FileNotFoundException, IOException
    {
        if (e.world.isRemote)
        {
            // ignore client stuff
            return;
        }
        
        File out = getFile(e.world);
        
        LinkedList<BloodStain> outStains = new LinkedList<BloodStain>();
        for (BloodStain stain : stains)
        {
            if (stain.dimId == e.world.provider.dimensionId)
            {
                outStains.add(stain);
            }
        }
        
        LOGGER.info("Writing {} BloodStains in world '{}'", outStains.size(), e.world.provider.getDimensionName());
        LOGGER.debug("Writing to file {}", out);
        
        saveStains(outStains, out);
    }

    @SubscribeEvent
    public void worldUnload(WorldEvent.Unload e) throws FileNotFoundException, IOException
    {
        if (e.world.isRemote)
        {
            // ignore client stuff
            return;
        }
        
        File out = getFile(e.world);
        
        LinkedList<BloodStain> outStains = new LinkedList<BloodStain>();
        Iterator<BloodStain> itStain = stains.iterator();
        while(itStain.hasNext())
        {
            BloodStain stain = itStain.next();
            if (stain.dimId == e.world.provider.dimensionId)
            {
                outStains.add(stain);
                itStain.remove();
            }
        }
        
        LOGGER.info("Writing {} BloodStains in world '{}'", outStains.size(), e.world.provider.getDimensionName());
        LOGGER.debug("Writing to file {}", out);
        
        saveStains(outStains, out);
    }

    private static void saveStains(List<BloodStain> stains, File f) throws FileNotFoundException, IOException
    {
        f.getParentFile().mkdirs();
        DataOutputStream stream = new DataOutputStream(new LzmaOutputStream.Builder(new FileOutputStream(f)).build());
        
        stream.writeInt(stains.size());
        for (BloodStain stain : stains)
        {
            LOGGER.debug("Writing stain at [{}, {}, {}] in dim {} with username {}", stain.x, stain.y, stain.z, stain.dimId, stain.username);
            stain.writeTo(stream);
        }
        
        stream.flush();
        stream.close();
    }
    
    private static File getFile(World world)
    {
        String worldSaveFolder = world.provider.getSaveFolder();
        if (worldSaveFolder == null)
        {
            worldSaveFolder = "";
        }
        else
        {
            worldSaveFolder = "/"+worldSaveFolder;
        }
        
        return new File(world.getSaveHandler().getWorldDirectory() + worldSaveFolder, FILE_NAME);
    }
}
