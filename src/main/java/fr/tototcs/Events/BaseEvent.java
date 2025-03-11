package fr.tototcs.Events;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

public class BaseEvent implements Listener {


    // List of modified Chunks
    public static final Set<String> modifiedChunks = new HashSet<>();

    // Add a new Chunk in the list
    private static String getChunkKey(Chunk chunk) {
        //System.out.println(chunk.getWorld().getName() + ":" + chunk.getX() + "," + chunk.getZ());
        return chunk.getWorld().getName() + ":" + chunk.getX() + "," + chunk.getZ();
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        modifiedChunks.add(getChunkKey(chunk));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        modifiedChunks.add(getChunkKey(chunk));
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        modifiedChunks.add(getChunkKey(chunk));
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        for (Block block : event.blockList()) {
            Chunk chunk = block.getChunk();
            modifiedChunks.add(getChunkKey(chunk));
        }
    }

    @EventHandler
    public void onBlockPhysics(BlockPhysicsEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        modifiedChunks.add(getChunkKey(chunk));
    }

    /*@EventHandler
    public void onBlockForm(BlockFormEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        modifiedChunks.add(getChunkKey(chunk));
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        modifiedChunks.add(getChunkKey(chunk));
    }*/

    @EventHandler
    public void onBlockGrow(BlockGrowEvent event) {
        Block block = event.getBlock();
        Chunk chunk = block.getChunk();
        modifiedChunks.add(getChunkKey(chunk));
    }

    @EventHandler
    public void onBlockPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            Chunk chunk = block.getChunk();
            modifiedChunks.add(getChunkKey(chunk));
        }
    }

    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            Chunk chunk = block.getChunk();
            modifiedChunks.add(getChunkKey(chunk));
        }
    }


    @EventHandler
    public void onEntityDamaged(EntityDamageEvent event){
        Chunk chunk = event.getEntity().getChunk();
        modifiedChunks.add(getChunkKey(chunk));
    }


}
