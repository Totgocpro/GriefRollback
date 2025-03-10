package fr.tototcs.ChunkStorage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import fr.tototcs.GriefRollback;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.intellij.lang.annotations.Subst;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ChunkBlockBytes {

    public static List<String> compressList(List<String> liste) {
        List<String> result = new ArrayList<>();
        String current = liste.get(0);
        int count = 1;

        for (int i = 1; i < liste.size(); i++) {
            if (liste.get(i).equals(current)) {
                count++;
            } else {
                if (count > 1) {
                    result.add(count + "|" + current);
                } else {
                    result.add(current);
                }
                current = liste.get(i);
                count = 1;
            }
        }

        // Ajout du dernier élément
        if (count > 1) {
            result.add(count + "|" + current);
        } else {
            result.add(current);
        }

        return result;
    }

    public static List<String> decompressList(List<String> liste) {
        List<String> result = new ArrayList<>();

        for (String element : liste) {
            if (element.contains("|")) {
                // Décomposition de l'élément en comptage et valeur
                String[] parts = element.split("\\|");
                int count = Integer.parseInt(parts[0]);
                String value = parts[1];

                // Ajout de l'élément à la liste de résultats 'count' fois
                for (int i = 0; i < count; i++) {
                    result.add(value);
                }
            } else {
                // Ajout de l'élément à la liste de résultats
                result.add(element);
            }
        }

        return result;
    }

    public static byte @NotNull [] getChunkBlockBytes(Chunk chunk) throws IOException {
        long startTime = System.nanoTime(); // Not used

        try {
            // Parcours des blocs du chunk
            int minHeight = chunk.getWorld().getMinHeight();
            int maxHeight = chunk.getWorld().getMaxHeight();

            List<String> blockDataList = new ArrayList<>();

            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = minHeight; y < maxHeight; y++) {
                        Block block = chunk.getBlock(x, y, z);
                        String blockDataString = block.getBlockData().getAsString();

                        // Delete prefix "minecraft:" for better compression
                        if (blockDataString.startsWith("minecraft:")) {
                            blockDataString = blockDataString.substring(10);
                        }

                        // Compress air block
                        if (blockDataString.equals("air")) {
                            blockDataString = "a";
                        }
                        // Store Chest content
                        if (block.getType() == Material.CHEST && GriefRollback.getInstance().getConfig().getBoolean("StoreChestContent", true)){
                            Chest chest = (Chest) block.getState();
                            if (chest.hasBeenFilled()){
                                try{
                                    blockDataString = "CHEST:"+ serializeInventory(chest.getBlockInventory());
                                } catch (Exception e) {
                                    throw new IOException();
                                }finally {
                                    blockDataString = blockDataString;
                                }

                            }
                        }

                        blockDataList.add(blockDataString + "\n");
                    }
                }
            }

            //compress list
            blockDataList = compressList(blockDataList);

            // Add Villagers
            if (GriefRollback.getInstance().getConfig().getBoolean("StoreVillagers", true)){
                for (Entity entity : chunk.getEntities()){
                    if (entity.getType() == EntityType.VILLAGER){
                        Villager villager = (Villager) entity;
                        //Format: "VILLAGER:[NAME;XPOS;YPOS;ZPOS;LEVEL;EXP;TYPE;PROFESSION]
                        String ParsedVillager = "VILLAGER:["
                                + entity.getName()+";"+
                                entity.getLocation().x()+";"+
                                entity.getLocation().y()+";"+
                                entity.getLocation().z()+";"+
                                villager.getVillagerLevel()+";"+
                                villager.getVillagerExperience()+";"+
                                villager.getVillagerType()+";"+
                                villager.getProfession()+";"+

                                "]";
                        blockDataList.add(ParsedVillager);
                    }
                }
            }

            // Convertir la liste de chaînes de caractères en octets
            StringBuilder blockDataBuilder = new StringBuilder();
            for (String blockData : blockDataList) {
                blockDataBuilder.append(blockData);
            }

            return blockDataBuilder.toString().getBytes(StandardCharsets.ISO_8859_1);

        } catch (Exception e) {
            return new byte[0];
        } finally {
            long endTime = System.nanoTime();
            //System.out.println("Execution time: " + (endTime - startTime) + " ns");
        }
    }

    private static final Gson gson = new Gson();
    // Convert Inventory to String
    public static String serializeInventory(Inventory inventory) {
        JsonArray jsonArray = new JsonArray();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            JsonObject itemJson = new JsonObject();
            itemJson.addProperty("slot", i);
            itemJson.addProperty("item", item != null ? gson.toJson(item.serialize()) : "null");
            jsonArray.add(itemJson);
        }
        return Base64.getEncoder().encodeToString(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static ItemStack[] deserializeInventory(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            String jsonString = new String(bytes, StandardCharsets.UTF_8);
            JsonArray jsonArray = gson.fromJson(jsonString, JsonArray.class);

            int size = jsonArray.size();
            ItemStack[] items = new ItemStack[size];

            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject itemJson = jsonArray.get(i).getAsJsonObject();
                int slot = itemJson.get("slot").getAsInt();
                if (!itemJson.get("item").getAsString().equals("null")) {
                    Map<String, Object> itemData = gson.fromJson(itemJson.get("item").getAsString(), new TypeToken<Map<String, Object>>() {}.getType());
                    items[slot] = ItemStack.deserialize(itemData);
                }
            }
            return items;
        } catch (Exception e) {
            return new ItemStack[0];
        }
    }

    /**
     * Calcule les coordonnées globales en fonction des coordonnées du chunk
     * et des coordonnées locales dans le chunk.
     *
     * @param chunkX  Coordonnée X du chunk.
     * @param chunkZ  Coordonnée Z du chunk.
     * @param localX  Coordonnée X locale dans le chunk (0 à 15).
     * @param localZ  Coordonnée Z locale dans le chunk (0 à 15).
     * @return Un tableau contenant les coordonnées globales [globalX, globalZ].
     * @throws IllegalArgumentException si les coordonnées locales sont en dehors de la plage [0, 15].
     */
    public static int[] getGlobalCoordinates(int chunkX, int chunkZ, int localX, int localZ) {
        // Vérifier que les coordonnées locales sont dans la plage valide
        if (localX < 0 || localX >= 16 || localZ < 0 || localZ >= 16) {
            throw new IllegalArgumentException("Les coordonnées locales doivent être comprises entre 0 et 15 inclus.");
        }

        // Calcul des coordonnées globales
        int globalX = chunkX * 16 + localX;
        int globalZ = chunkZ * 16 + localZ;

        return new int[] { globalX, globalZ };
    }

    public static List<String> convertBytesToLines(byte[] bytes) {
        // Décoder les bytes en chaîne en utilisant ISO-8859-1
        String decodedString = new String(bytes, StandardCharsets.ISO_8859_1);

        // Diviser la chaîne en lignes
        String[] lines = decodedString.split("\\R"); // \\R correspond à n'importe quel séparateur de ligne

        // Convertir en liste
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            result.add(line);
        }

        return result;
    }


    public static void loadChunkFromBytes(byte[] chunkData, int chunkx, int chunkz, World world) throws IOException {
        // Prevent blank files
        if (chunkData.length == 0){
            return;
        }

        int minHeight = world.getMinHeight();
        int maxHeight = world.getMaxHeight();

        //Convertir la liste en string
        List<String> data = convertBytesToLines(chunkData);

        //Décompresser la liste
        data = decompressList(data);

        int i = 0;
        String minecraftPrefix = "minecraft:"; // mc prefix

        // Utilisation d'un cache pour éviter les appels répétés de 'Bukkit.createBlockData'
        Map<String, BlockData> blockDataCache = new HashMap<>();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minHeight; y < maxHeight; y++) {
                    String blockDataString = data.get(i++);

                    // a --> air block
                    if (blockDataString.equals("a")) {
                        blockDataString = minecraftPrefix + "air";
                    }

                    // Place Chest with data
                    if (blockDataString.startsWith("CHEST:")){
                        String DataWithNotPrefix = blockDataString.substring(6);
                        BlockData block = Bukkit.createBlockData(Material.CHEST);

                        int[] coordinates = getGlobalCoordinates(chunkx, chunkz, x, z);
                        Location location = new Location(world, coordinates[0], y, coordinates[1]);
                        location.getBlock().setBlockData(block);
                        Chest chest = (Chest) location.getBlock().getState();
                        Inventory inventory = chest.getInventory();


                        inventory.clear();
                        int e = 0;
                        for (ItemStack item : deserializeInventory(DataWithNotPrefix)){
                            inventory.setItem(e, item);
                            e++;
                        }
                        continue;
                    }

                    // Si le bloc n'a pas le préfixe 'minecraft:', on l'ajoute
                    if (!blockDataString.startsWith(minecraftPrefix)) {
                        blockDataString = minecraftPrefix + blockDataString;
                    }

                    // Vérification du cache pour ne pas recréer le BlockData à chaque fois
                    BlockData blockData = blockDataCache.computeIfAbsent(blockDataString, Bukkit::createBlockData);

                    // Get global coordinates
                    int[] coordinates = getGlobalCoordinates(chunkx, chunkz, x, z);
                    Location location = new Location(world, coordinates[0], y, coordinates[1]);

                    // Change block data
                    location.getBlock().setBlockData(blockData);
                }
            }
        }

        // Add villagers and other data
        for (String Data : data){
            if (Data.startsWith("VILLAGER:[")){
                //Format: "VILLAGER:[NAME;XPOS;YPOS;ZPOS;LEVEL;EXP;TYPE;PROFESSION]
                String DataWithNotPrefix = Data.substring(9);
                @Subst("VillagerValue") List<String> VillagerData = Arrays.asList(DataWithNotPrefix.replaceAll("[\\[\\]]", "").split(";"));
                Villager villager = (Villager) world.spawnEntity(new Location(world, Double.parseDouble(VillagerData.get(1)), Double.parseDouble(VillagerData.get(2)), Double.parseDouble(VillagerData.get(3))), EntityType.VILLAGER);
                villager.setCustomNameVisible(true);
                if (!VillagerData.get(0).equals("Villager")){
                    ((Nameable) villager).customName(Component.newline().content(VillagerData.getFirst()));
                }
                villager.setVillagerLevel(Integer.parseInt(VillagerData.get(4)));
                villager.setVillagerExperience(Integer.parseInt(VillagerData.get(5)));
                villager.setVillagerType(Objects.requireNonNull(Registry.VILLAGER_TYPE.get(NamespacedKey.minecraft(VillagerData.get(6).toLowerCase()))));
                villager.setProfession(Objects.requireNonNull(Registry.VILLAGER_PROFESSION.get(NamespacedKey.minecraft(VillagerData.get(7).toLowerCase()))));
                villager.setAI(true);
            }

        }

    }

}
