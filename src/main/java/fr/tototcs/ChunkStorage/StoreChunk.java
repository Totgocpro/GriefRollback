package fr.tototcs.ChunkStorage;

import fr.tototcs.GriefRollback;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.*;

import java.nio.file.*;

import java.nio.file.Paths;

import static fr.tototcs.ChunkStorage.ChunkBlockBytes.getChunkBlockBytes;
import static fr.tototcs.ChunkStorage.ChunkBlockBytes.loadChunkFromBytes;
import static fr.tototcs.ChunkStorage.ChunkStorageAtTick.deleteFolderRecursively;
import static fr.tototcs.ChunkStorage.ChunkStorageAtTick.zipFolder;

public class StoreChunk {
    public static void saveChunk(Chunk chunk, long CheckPointID) {
        String path = chunk.getX() + "_" + chunk.getZ() + ".grsave";


        // Dossier du plugin (remplacez ceci par le chemin réel du dossier du plugin)
        String pluginDirectory = "plugins/GriefRollback/Checkpoints/" + CheckPointID + "/" + chunk.getWorld().getName() + "/"; // Ex : "plugins/MyPlugin" (mettre à jour selon votre environnement)

        // Crée le chemin complet du fichier de sauvegarde
        Path filePath = Paths.get(pluginDirectory, path);

        try {
            // Crée le dossier si il n'existe pas
            Files.createDirectories(filePath.getParent());

            // Récupère les données du chunk sous forme de bytes
            byte[] chunkData = getChunkBlockBytes(chunk);

            // Save data in a file
            try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
                bos.write(chunkData);
            }

        } catch (IOException e) {
            e.printStackTrace();

        }
    }

    public static Map<String, File> listFiles(String baseDir, long timestampReference, String world) {
        File baseDirectory = new File(baseDir);
        Map<String, File> fileMap = new HashMap<>();

        // Vérifier si le dossier de base existe
        if (baseDirectory.exists() && baseDirectory.isDirectory()) {
            // Lister tous les dossiers dans le répertoire principal
            File[] timestampDirs = baseDirectory.listFiles(File::isFile);
            if (timestampDirs != null) {
                for (File timestampDir : timestampDirs) {
                    // Vérifier si le dossier a un nom de timestamp valide et s'il est inférieur ou égal au timestamp de référence
                    try {
                        String substring = timestampDir.getName().substring(0, timestampDir.getName().length() - 4);
                        long dirTimestamp = Long.parseLong(substring);

                        if (dirTimestamp <= timestampReference) {

                            try {
                                unzip(timestampDir, new File(baseDir + substring));
                            } catch (IOException e) {

                                throw new RuntimeException(e);
                            }

                            // Chemin vers le dossier correspondant au "world"
                            File worldDir = new File(baseDir + substring + "/"+ substring + "/" + world);
                            if (worldDir.exists() && worldDir.isDirectory()) {
                                // Lister tous les fichiers dans le dossier "world"
                                File[] files = worldDir.listFiles(File::isFile);
                                if (files != null) {
                                    for (File file : files) {
                                        // Remplacer les fichiers plus anciens par ceux plus récents
                                        fileMap.put(file.getName(), file);
                                    }
                                }
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignorer les dossiers qui n'ont pas de timestamp valide
                    }
                }
            }
        }

        return fileMap;
    }


    public static void deleteDirectoriesOnly(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // Supprime récursivement le contenu des dossiers
                    deleteDirectoriesOnly(file);
                    // Supprime le dossier s'il est vide après le nettoyage
                    if (Objects.requireNonNull(file.listFiles()).length == 0) {
                        file.delete();
                    }
                } else {
                    // Supprime le fichier s'il n'a pas l'extension .grs
                    if (!file.getName().endsWith(".grs")) {
                        file.delete();
                    }
                }
            }
        }
    }


    public static Chunk getChunkFromKey(String chunkKey, Server server) {
        // Diviser la chaîne clé pour obtenir le nom du monde et les coordonnées
        String[] parts = chunkKey.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid chunk key format: " + chunkKey);
        }

        String worldName = parts[0];
        String[] coordinates = parts[1].split(",");
        if (coordinates.length != 2) {
            throw new IllegalArgumentException("Invalid coordinates format in chunk key: " + chunkKey);
        }

        try {
            // Convertir les coordonnées en entiers
            int x = Integer.parseInt(coordinates[0]);
            int z = Integer.parseInt(coordinates[1]);

            // Obtenir le monde à partir du nom
            World world = server.getWorld(worldName);
            if (world == null) {
                throw new IllegalArgumentException("World not found: " + worldName);
            }

            // Retourner le chunk correspondant
            return world.getChunkAt(x, z);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format in chunk key: " + chunkKey, e);
        }
    }

    public static void unzip(File zipFile, File destinationFolder) throws IOException {
        if (!destinationFolder.exists()) {
            destinationFolder.mkdirs(); // Crée le dossier de destination s'il n'existe pas
        }

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File newFile = new File(destinationFolder, entry.getName());

                // Vérifie si c'est un dossier
                if (entry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    // Crée les dossiers parents si nécessaire
                    new File(newFile.getParent()).mkdirs();

                    // Écrit le contenu du fichier
                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zis.closeEntry();
            }
        }
    }


    public static void SaveListOfChunk(Set<String> ChunkList, Player playerask, boolean OneTime) {
        if (OneTime){
            // save the chunk with no chunk separation
            ArrayList<String> Chunksstr = new ArrayList<>(ChunkList);
            long CheckPointID = System.currentTimeMillis();
            if (!ChunkList.isEmpty()){
                int s = 0;
                for (String i : Chunksstr){
                    s++;
                    Chunk chunk = StoreChunk.getChunkFromKey(i, Bukkit.getServer());
                    StoreChunk.saveChunk(chunk, CheckPointID);
                    GriefRollback.getInstance().getLogger().log(Level.INFO, "Saving... [" + s+"/"+Chunksstr.size()+"]");
                }
                File sourceFolder = new File("plugins/GriefRollback/Checkpoints/" + CheckPointID);
                File zipFile = new File("plugins/GriefRollback/Checkpoints/" + CheckPointID + ".grs");
                try {
                    zipFolder(sourceFolder, zipFile);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                deleteFolderRecursively(sourceFolder);
                GriefRollback.getInstance().getLogger().log(Level.INFO,"Saved");
            }
        }else{
            ChunkStorageAtTick.SaveChunks(ChunkList, playerask);
        }
    }


    public static void loadChunkFromFile(int x, int z, World world, String Path) {
        // Dossier du plugin (mettez à jour selon votre environnement)
        //String pluginDirectory = Path; // Ex : "plugins/MyPlugin"

        // Crée le chemin complet du fichier de sauvegarde
        Path filePath = Paths.get(Path);

        // Vérifie si le fichier existe
        if (!Files.exists(filePath)) {
            System.out.println("Le fichier de sauvegarde du chunk n'existe pas à : " + filePath.toString());
            return;
        }

        try {
            // Lis les données du chunk à partir du fichier
            byte[] chunkData = Files.readAllBytes(filePath);

            // Charge les blocs dans le monde
            loadChunkFromBytes(chunkData, x, z, world);

            //System.out.println("Chunk chargé à partir de : " + filePath.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



}
