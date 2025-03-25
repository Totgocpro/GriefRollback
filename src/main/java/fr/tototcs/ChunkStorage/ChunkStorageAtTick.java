package fr.tototcs.ChunkStorage;

import fr.tototcs.Checkpoints.CheckpointsCleaner;
import fr.tototcs.Events.BaseEvent;
import fr.tototcs.GriefRollback;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.io.*;
import java.util.zip.*;

public class ChunkStorageAtTick {
    static boolean isTask = false;
    static String Type = "";
    static long CheckPointID = System.currentTimeMillis();
    static List<String> Chunksstr = null;
    static int i = 0;

    public static Player getPlayerask() {
        return playerask;
    }

    public static void setPlayerask(Player playerask) {
        ChunkStorageAtTick.playerask = playerask;
    }

    static Player playerask = null;
    static Map<String, File> fileMap;
    static List<String> fileList;
    static World world;

    //Fonction qui permet de récupérer des infos sur la tache en cour
    public static String getTask(){
        if (isTask){
            return Type;
        }else {
            return null;
        }
    }

    public static void SaveChunks(Set<String> val, Player askplayer){
        if (!isTask) {
            if (val.isEmpty()){
                if (askplayer != null) {
                    askplayer.sendMessage("§4[GriefRollback] §r§2Task did not start: no chunk modified");
                }
                return;
            }
            CheckPointID = System.currentTimeMillis();
            i = 0;
            Chunksstr = new ArrayList<>(val);
            playerask = askplayer;
            Type = "Save";

            isTask = true;
        }else {
            if (askplayer != null) {askplayer.sendMessage("§4[GriefRollback] §r§2A task was already started");}
        }
    }

    // Méthode principale pour zipper un dossier
    public static void zipFolder(File sourceFolder, File zipFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            zipFolderRecursively(sourceFolder, sourceFolder.getName(), zos);
        }
    }

    // Méthode récursive pour parcourir les dossiers et ajouter les fichiers au zip
    private static void zipFolderRecursively(File folder, String parentFolderName, ZipOutputStream zos) throws IOException {
        File[] files = folder.listFiles();
        if (files == null || files.length == 0) {
            // Ajouter une entrée pour les dossiers vides
            zos.putNextEntry(new ZipEntry(parentFolderName + "/"));
            zos.closeEntry();
            return;
        }

        for (File file : files) {
            String entryName = parentFolderName + "/" + file.getName();
            if (file.isDirectory()) {
                zipFolderRecursively(file, entryName, zos);
            } else {
                zipFile(file, entryName, zos);
            }
        }
    }

    // Méthode pour ajouter un fichier au zip
    private static void zipFile(File file, String entryName, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry zipEntry = new ZipEntry(entryName);
            zos.putNextEntry(zipEntry);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }

            zos.closeEntry();
        }
    }

    // Function to delete a Folder
    public static void deleteFolderRecursively(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolderRecursively(file);
                } else {
                    file.delete(); // Delete file
                }
            }
        }
        folder.delete(); // Delete the folder
    }

    public static void LoadChunk(Long time, World currentworld, Player askplayer, double radius){
        if (!isTask) {
            if (askplayer != null) {
                askplayer.sendMessage("§4[GriefRollback] §r§2Preparation (can cause a lot of tps loss)");
            }
            CheckPointID = time;
            i = 0;
            Chunksstr = null;
            fileMap = StoreChunk.listFiles("plugins/GriefRollback/Checkpoints/", time, currentworld.getName());
            fileList = new ArrayList<>(fileMap.keySet());
            playerask = askplayer;
            Type = "Load";
            world = currentworld;
            if (!fileList.isEmpty()) {
                isTask = true;
            }
        }else {
            if (askplayer != null) {
                askplayer.sendMessage("§4[GriefRollback] §r§2A task was already started");
            }
        }
    }

    public static void AtTick() throws IOException {
        if (isTask){
            if (Type.equals("Save")) {
                String chunkstr = Chunksstr.get(i);

                Chunk chunk = StoreChunk.getChunkFromKey(chunkstr, Bukkit.getServer());
                StoreChunk.saveChunk(chunk, CheckPointID);
                BaseEvent.modifiedChunks.remove(chunkstr);
                if (playerask != null) {
                    playerask.sendMessage("§4[GriefRollback] §r§2Saving... Working on the chunk ["+(i+1) + "/"+Chunksstr.size() + "]");
                }
                i++;

                if (i >= Chunksstr.size()) {
                    isTask = false;

                    File sourceFolder = new File("plugins/GriefRollback/Checkpoints/" + CheckPointID);
                    File zipFile = new File("plugins/GriefRollback/Checkpoints/" + CheckPointID + ".grs");
                    zipFolder(sourceFolder, zipFile);
                    deleteFolderRecursively(sourceFolder);

                    if (GriefRollback.getInstance().getConfig().getBoolean("Autodeleteversion", true)){
                        playerask.sendMessage("§4[GriefRollback] §r§2Deleting old version...");
                        CheckpointsCleaner.cleanOldGrsFiles("plugins/GriefRollback/Checkpoints/", GriefRollback.getInstance().getConfig().getLong("AutodeleteversionMaxSize", 5000));
                    }

                    if (playerask != null) {
                        playerask.sendMessage("§4[GriefRollback] §r§2A CheckPoint was created !");
                    }
                }
            }

            if (Type.equals("Load")) {
                String filename = "";
                if (!(i >= fileList.size())) {
                    filename = fileList.get(i);
                }else {
                    return;
                }

                String oldfilename = filename;
                if (filename.isEmpty()) {
                    return; // Retourne la chaîne originale si elle est nulle ou vide
                }


                // Trouver la position du dernier point
                int dotIndex = filename.lastIndexOf(".");

                // Si le point est trouvé, on extrait le nom sans l'extension
                if (dotIndex != -1) {
                    filename = filename.substring(0, dotIndex);
                }

                String[] parts = filename.split("_");

                StoreChunk.loadChunkFromFile(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), world, fileMap.get(oldfilename).getAbsolutePath());

                i++;
                assert playerask != null;
                playerask.sendMessage("§4[GriefRollback] §r§2Working on the chunk ["+(i) + "/"+fileList.size() + "]");


                if (i >= fileMap.size()) {
                    isTask = false;
                    assert playerask != null;
                    playerask.sendMessage("§4[GriefRollback] §r§2Cleaning, can create tps drops!");

                    File directory = new File("plugins/GriefRollback/Checkpoints/");

                    StoreChunk.deleteDirectoriesOnly(directory);

                    assert playerask != null;
                    playerask.sendMessage("§4[GriefRollback] §r§2The world was rollback");
                }
            }


        }
    }

}
