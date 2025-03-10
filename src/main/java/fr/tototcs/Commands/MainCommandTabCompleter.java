package fr.tototcs.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MainCommandTabCompleter implements TabCompleter {

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1){ 
            suggestions.add("save");
            suggestions.add("rollback");
            suggestions.add("task");
            suggestions.add("versions");

            return suggestions;
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("rollback")){
            suggestions.add("1s");
            suggestions.add("1m");
            suggestions.add("1h");
            suggestions.add("1d");
            suggestions.add("1w");
            suggestions.add("1mo");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("task")){
            suggestions.add("info");
            suggestions.add("join");
        }

        return suggestions;
    }
}
