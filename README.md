**Protect your server from griefing by being able to restore and revert to your world !**

How it works: Allows you to save control points of your world at certain predefined times and restore them in case of problems in a single command.

✅ Backups are optimized and are designed to take up little disk space by only saving what is necessary.

✅ Open-source

✅ Tps only decreases when saving and rolling back chunks.

✅ Configurable

🐛 Found a bug? [Issue page](https://github.com/Totgocpro/GriefRollback/issues)

[Source code](https://github.com/Totgocpro/GriefRollback)


<details>
<summary>Commands</summary>

  - griefrollback save → Perform a world backup
  - griefrollback task [info/join] → Get current task info or ask the plugin to set itself as the task executor (used to get real-time progress).
  - griefrollback rollback [time] → Restores the world as it was [time] ago (it will restore the nearest backup).

</details>


<details>
<summary>Config File</summary>

#This is the main configuration file for GriefRollback

#If Auto-checkpoint is equal true, a schedule start and the world will be save at a defined tick interval.
AutoCheckpoint: true
#The time interval in ticks where all modified chunks will be saved (Too low a number can greatly impact server performance) (https://codepen.io/mrjohndoe69/pen/ExPZpNb)
AutoCheckpointInterval: 72000
#If true all auto-checkpoint task will be sent into the server console
LogAutoCheckPoint: true

#If set to true all villagers, xp, position and profession will be save (can take more time to save a chunk and more space and the Trades was not saved currently)
StoreVillagers: true

#If set to true store all items inside the Chest (can take more space to save)
StoreChestContent: true

</details>
