package fr.tototcs.Commands;

import fr.tototcs.ChunkStorage.ChunkBlockBytes;
import fr.tototcs.ChunkStorage.ChunkStorageAtTick;
import fr.tototcs.ChunkStorage.StoreChunk;
import fr.tototcs.Events.BaseEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class MainCommand implements CommandExecutor {

    public static List<String> getLastGRSFiles(String directoryPath, int x, int Offset) {
        File folder = new File(directoryPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return new ArrayList<>();
        }

        return Arrays.stream(Objects.requireNonNull(folder.listFiles((dir, name) -> name.endsWith(".grs"))))
                .map(File::getName)
                .sorted((a, b) -> Long.compare(Long.parseLong(b.replace(".grs", "")), Long.parseLong(a.replace(".grs", ""))))
                .skip(Offset)
                .limit(x)
                .collect(Collectors.toList());
    }

    public static int getMaxOffset(String directoryPath, int x) {
        File folder = new File(directoryPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return 0;
        }

        // Compter le nombre total de fichiers .grs
        long totalFiles = Objects.requireNonNull(folder.listFiles((dir, name) -> name.endsWith(".grs"))).length;

        // Calculer le maximum d'offset possible
        return Math.max(0, (int)Math.ceil((double) totalFiles / x));
    }
    public static String convertTimestamp(String timestampStr) {
        try {

            long timestamp = Long.parseLong(timestampStr);

            // Vérification si la timestamp est en secondes ou millisecondes
            if (timestampStr.length() == 10) {
                timestamp *= 1000; // Convertir en millisecondes
            } else if (timestampStr.length() != 13) {
                throw new IllegalArgumentException("Not correct");
            }

            // Conversion en date et formatage
            Instant instant = Instant.ofEpochMilli(timestamp);
            ZonedDateTime dateTime = instant.atZone(ZoneId.systemDefault());

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            return dateTime.format(formatter);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException();
        }

    }

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
    public static String formatTimestamp(long timestamp) {
        final long SECOND = 1000;
        final long MINUTE = 60 * SECOND;
        final long HOUR = 60 * MINUTE;
        final long DAY = 24 * HOUR;
        final long WEEK = 7 * DAY;
        final long MONTH = 30 * DAY; // Approximatif

        long currentTimestamp = System.currentTimeMillis();
        long elapsedTime = currentTimestamp - timestamp;

        if (elapsedTime < 0) {
            throw new IllegalArgumentException("Future timeStamp !");
        }

        Map<Long, String> timeUnits = new LinkedHashMap<>();
        timeUnits.put(MONTH, "mo");
        timeUnits.put(WEEK, "w");
        timeUnits.put(DAY, "d");
        timeUnits.put(HOUR, "h");
        timeUnits.put(MINUTE, "m");
        timeUnits.put(SECOND, "s");

        StringBuilder formattedTime = new StringBuilder();

        for (Map.Entry<Long, String> entry : timeUnits.entrySet()) {
            long unitTime = entry.getKey();
            String unitSymbol = entry.getValue();

            long count = elapsedTime / unitTime;
            if (count > 0) {
                formattedTime.append(count).append(unitSymbol);
                elapsedTime %= unitTime;
            }
        }

        return formattedTime.toString();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String msg, @NotNull String[] args) {
        if (sender instanceof Player){
            switch (args[0]) {
                case "save" -> StoreChunk.SaveListOfChunk(BaseEvent.modifiedChunks, (Player) sender, false);
                case "rollback" -> {
                    if (args.length == 2) {
                        ChunkStorageAtTick.LoadChunk(parseToTimestamp(args[1]), ((Player) sender).getWorld(), (Player) sender);
                    }
                }
                case "task" -> {
                    if (args.length == 2) {
                        if (args[1].equals("info")) {
                            String task = ChunkStorageAtTick.getTask();
                            if (task == null) {
                                sender.sendMessage("§4[GriefRollback] §r§2No task was started");
                            } else {
                                if (ChunkStorageAtTick.getPlayerask() != null) {
                                    sender.sendMessage("§4[GriefRollback] §r§2A task was started by " + ChunkStorageAtTick.getPlayerask().getName());
                                } else {
                                    sender.sendMessage("§4[GriefRollback] §r§2A task was started by Server");
                                }
                            }
                        } else if (args[1].equals("join")) {
                            String task = ChunkStorageAtTick.getTask();
                            if (task == null) {
                                sender.sendMessage("§4[GriefRollback] §r§2No task was started");
                            } else {
                                if (ChunkStorageAtTick.getPlayerask() != null) {
                                    sender.sendMessage("§4[GriefRollback] §r§2Only server task can be joined");
                                } else {
                                    ChunkStorageAtTick.setPlayerask((Player) sender);
                                    sender.sendMessage("§4[GriefRollback] §r§2You have be set by the author of the task");
                                }
                            }
                        }
                    }else{
                        String task = ChunkStorageAtTick.getTask();
                        if (task == null) {
                            sender.sendMessage("§4[GriefRollback] §r§2No task was started");
                        } else {
                            if (ChunkStorageAtTick.getPlayerask() != null) {
                                sender.sendMessage("§4[GriefRollback] §r§2A task was started by " + ChunkStorageAtTick.getPlayerask().getName());
                            } else {
                                sender.sendMessage("§4[GriefRollback] §r§2A task was started by Server");
                            }
                        }
                    }
                }
                case "versions" -> {
                    int Offset = 0;
                    if (args.length != 1) {
                        Offset = Integer.parseInt(args[1]);
                    }

                    if (getLastGRSFiles("plugins/GriefRollback/Checkpoints/", 30, 0).isEmpty()){
                        sender.sendMessage("§4GriefRollback §r§2No Checkpoints was found");
                        return false;
                    }
                    sender.sendMessage("§4GriefRollback §r§2checkpoints Page [" + ((Offset / 10) + 1) + "/" + getMaxOffset("plugins/GriefRollback/Checkpoints/", 10) + "]");
                    for (String i : getLastGRSFiles("plugins/GriefRollback/Checkpoints/", 10, Offset)) {
                        Component text = Component.text("Checkpoint --> " + convertTimestamp(i.replaceAll(".grs", "")))
                                .color(NamedTextColor.RED)
                                .hoverEvent(HoverEvent.showText(Component.text("Click to see rollback command")))
                                .clickEvent(ClickEvent.suggestCommand("/gf rollback " + formatTimestamp(Long.parseLong(i.replaceAll(".grs", "")) + 5)));

                        sender.sendMessage(text);
                    }
                    Component PageNextComp = Component.text(">> Next Page")
                            .color(NamedTextColor.BLUE)
                            .hoverEvent(HoverEvent.showText(Component.text("Next")))
                            .clickEvent(ClickEvent.runCommand("/gf versions " + (Offset + 10)));
                    if (!(((Offset / 10) + 1) == getMaxOffset("plugins/GriefRollback/Checkpoints/", 10))) {
                        sender.sendMessage(PageNextComp);
                    }

                    Component PagePrevComp = Component.text("<< Previous Page")
                            .color(NamedTextColor.BLUE)
                            .hoverEvent(HoverEvent.showText(Component.text("Previous")))
                            .clickEvent(ClickEvent.runCommand("/gf versions " + (Offset - 10)));

                    if (!(Offset <= 0)) {
                        sender.sendMessage(PagePrevComp);
                    }
                }
            }
        }
        return false;
    }
}


