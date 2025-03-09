package fr.tototcs.Commands;

import fr.tototcs.ChunkStorage.ChunkBlockBytes;
import fr.tototcs.ChunkStorage.ChunkStorageAtTick;
import fr.tototcs.ChunkStorage.StoreChunk;
import fr.tototcs.Events.BaseEvent;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainCommand implements CommandExecutor {

    public static long parseToTimestamp(String timeString) {
        // Durées en millisecondes
        final long SECOND = 1000;
        final long MINUTE = 60 * SECOND;
        final long HOUR = 60 * MINUTE;
        final long DAY = 24 * HOUR;
        final long WEEK = 7 * DAY;
        final long MONTH = 30 * DAY; // Approximatif : 30 jours par mois

        long totalMilliseconds = 0;

        
        Pattern pattern = Pattern.compile("(\\d+)([smhdwmo])");
        Matcher matcher = pattern.matcher(timeString);

        
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            // Convert in Milliseconds
            switch (unit) {
                case "s":
                    totalMilliseconds += value * SECOND;
                    break;
                case "m":
                    totalMilliseconds += value * MINUTE;
                    break;
                case "h":
                    totalMilliseconds += value * HOUR;
                    break;
                case "d":
                    totalMilliseconds += value * DAY;
                    break;
                case "w":
                    totalMilliseconds += value * WEEK;
                    break;
                case "mo":
                    totalMilliseconds += value * MONTH;
                    break;
                default:
                    throw new IllegalArgumentException("Unité inconnue : " + unit);
            }
        }

        // Timestamp calc
        long currentTimestamp = System.currentTimeMillis();
        return currentTimestamp - totalMilliseconds;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String msg, @NotNull String[] args) {
        if (sender instanceof Player){
            if (args[0].equals("save")) {
                StoreChunk.SaveListOfChunk(BaseEvent.modifiedChunks, (Player) sender, false);

            } else if (args[0].equals("rollback")) {
                if (args.length == 2){
                    ChunkStorageAtTick.LoadChunk(parseToTimestamp(args[1]), ((Player) sender).getWorld(), (Player) sender);

                }
                //StoreChunk.loadChunkFromFile(((Player) sender).getChunk().getX(), ((Player) sender).getChunk().getZ(), ((Player) sender).getWorld());
                
            } else if (args[0].equals("task")) {
                if (args.length == 2){
                    if (args[1].equals("info")){
                        String task = ChunkStorageAtTick.getTask();
                        if (task == null){
                            sender.sendMessage("§4[GriefRollback] §r§2No task was started");
                        }else{
                            if (ChunkStorageAtTick.getPlayerask() != null) {
                                sender.sendMessage("§4[GriefRollback] §r§2A task was started by " + ChunkStorageAtTick.getPlayerask().getName());
                            }else {
                                sender.sendMessage("§4[GriefRollback] §r§2A task was started by Server");
                            }
                        }
                    }else if (args[1].equals("join")){
                        String task = ChunkStorageAtTick.getTask();
                        if (task == null){
                            sender.sendMessage("§4[GriefRollback] §r§2No task was started");
                        }else{
                            if (ChunkStorageAtTick.getPlayerask() != null) {
                                sender.sendMessage("§4[GriefRollback] §r§2Only server task can be joined");
                            }else {
                                ChunkStorageAtTick.setPlayerask((Player) sender);
                                sender.sendMessage("§4[GriefRollback] §r§2You have be set by the author of the task");
                            }
                        }
                    }
                }

            }
        }
        return false;
    }
}


