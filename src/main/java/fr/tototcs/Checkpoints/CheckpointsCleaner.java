package fr.tototcs.Checkpoints;

import fr.tototcs.GriefRollback;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;

public class CheckpointsCleaner {
    public static void cleanOldGrsFiles(String directoryPath, long maxSize) {

        maxSize = maxSize * 1024 * 1024;

        File directory = new File(directoryPath);
        if (!directory.isDirectory()) {
            return;
        }

        File[] grsFiles = directory.listFiles((dir, name) -> name.endsWith(".grs"));
        if (grsFiles == null || grsFiles.length == 0) {
            return;
        }

        Arrays.sort(grsFiles, Comparator.comparingLong(CheckpointsCleaner::getTimestamp));

        long totalSize = Arrays.stream(grsFiles).mapToLong(File::length).sum();

        for (File file : grsFiles) {
            if (totalSize <= maxSize) {
                break;
            }
            totalSize -= file.length();
            file.delete();
        }
    }

    private static long getTimestamp(File file) {
        try {
            return Long.parseLong(file.getName().replace(".grs", ""));
        } catch (NumberFormatException e) {
            return Long.MAX_VALUE;
        }
    }
}
