package fr.tototcs.Checkpoints;

import fr.tototcs.ChunkStorage.StoreChunk;
import fr.tototcs.Events.BaseEvent;
import fr.tototcs.GriefRollback;

import java.util.logging.Level;

public class AutoSaveAtTick {
    private static int Time;
    private static int interval;
    private static Boolean LogAutoCheckPoint;

    public static void Setup(){
        Time = 0;
        interval = GriefRollback.getInstance().getConfig().getInt("AutoCheckpointInterval", 72000);
        LogAutoCheckPoint = GriefRollback.getInstance().getConfig().getBoolean("LogAutoCheckPoint", true);
    }

    public static void AtTick(){
        Time++;
        if (Time >= interval){
            Time = 0;
            StoreChunk.SaveListOfChunk(BaseEvent.modifiedChunks, null, false);
            if (LogAutoCheckPoint){
                GriefRollback.getInstance().getLogger().log(Level.INFO, "A Save Task was been launch");
            }
        }
    }

}
