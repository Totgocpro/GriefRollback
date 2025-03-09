package fr.tototcs;

import fr.tototcs.Checkpoints.AutoSaveAtTick;
import fr.tototcs.ChunkStorage.ChunkStorageAtTick;
import fr.tototcs.ChunkStorage.StoreChunk;
import fr.tototcs.Commands.MainCommand;
import fr.tototcs.Commands.MainCommandTabCompleter;
import fr.tototcs.Events.BaseEvent;
import fr.tototcs.bstat.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;

public final class GriefRollback extends JavaPlugin {

    private static GriefRollback instance;


    @Override
    public void onEnable() {
        instance = this;

        Metrics metrics = new Metrics(this, 25029);



        AutoSaveAtTick.Setup();


        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            public static boolean AutoSave = GriefRollback.getInstance().getConfig().getBoolean("AutoCheckpoint", true);
            @Override
            public void run() {
                try {
                    ChunkStorageAtTick.AtTick();
                    if (AutoSave){
                        AutoSaveAtTick.AtTick();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }, 0L, 1L);

        //Save the config
        saveDefaultConfig();




        Objects.requireNonNull(getCommand("griefrollback")).setExecutor(new MainCommand());
        Objects.requireNonNull(getCommand("griefrollback")).setTabCompleter(new MainCommandTabCompleter());


        getServer().getPluginManager().registerEvents(new BaseEvent(), this);

        this.getLogger().log(Level.INFO, "GriefRollback was started successfully");

    }

    @Override
    public void onDisable() {
        // Create a Checkpoint at the server close
        StoreChunk.SaveListOfChunk(BaseEvent.modifiedChunks, null, true);


    }

    public static GriefRollback getInstance(){
        return instance;
    }
}
