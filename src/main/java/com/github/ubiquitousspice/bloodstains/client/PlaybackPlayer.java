package com.github.ubiquitousspice.bloodstains.client;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.stats.StatBase;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;

import com.github.ubiquitousspice.bloodstains.data.BloodStain;
import com.github.ubiquitousspice.bloodstains.data.PlayerState;
import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;

public class PlaybackPlayer extends EntityOtherPlayerMP
{
    private List<PlayerState> states;
    
    public PlaybackPlayer(BloodStain stain)
    {
        super(Minecraft.getMinecraft().thePlayer.worldObj, new GameProfile("---", "TEST"));
        this.posX = stain.x;
        this.posY = stain.y;
        this.posZ = stain.z;
        
        // convert states to a list of things that can eb safely popped off...
        states = new LinkedList<PlayerState>();
        states.addAll(stain.states);
        states = Lists.reverse(states);
        
        System.out.println("STARTING WITH SIZE="+states.size());
        
        this.dataWatcher = new DummyDataWatcher(this);
    }
    
    @Override
    public void onUpdate()
    {
        if (states.isEmpty())
        {
            // clean out.
            this.setDead();
            return;
        }
        
//        for (int i = 0; i < 3; i++)
//        {
//            Minecraft.getMinecraft().theWorld.spawnParticle("smoke", posX, posY, posZ, 0.0D, 0.1D, 0.0D);
//        }
        
        PlayerState state = states.remove(0);
        //System.out.println("Ticking --> "+states.size());
        state.applyTo(this);
        
        super.onUpdate();
    }
    
    // proper overrides
    
    @Override
    public void setHealth(float par1) { }

    // stolen from forge fakePlayer + some edits
    @Override protected void entityInit() {}
    @Override public boolean attackEntityFrom(DamageSource source, float par2)  { return false;  }
    @Override public void addChatMessage(IChatComponent chat) {}
    @Override public void addChatComponentMessage(IChatComponent chat){}
    @Override public void addStat(StatBase par1StatBase, int par2){}
    @Override public void openGui(Object mod, int modGuiId, World world, int x, int y, int z){}
    @Override public boolean isEntityInvulnerable(){ return true; }
    @Override public boolean canAttackPlayer(EntityPlayer player){ return false; }
    @Override public boolean hitByEntity(Entity par1Entity) { return false; }
    @Override public void onDeath(DamageSource source){ return; }
    @Override public void travelToDimension(int dim){ return; }
    @Override public boolean canCommandSenderUseCommand(int var1, String var2) { return false; }
}