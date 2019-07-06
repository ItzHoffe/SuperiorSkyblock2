package com.bgsoftware.superiorskyblock.commands.command;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPermission;
import com.bgsoftware.superiorskyblock.api.island.IslandRole;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.wrappers.SSuperiorPlayer;
import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.commands.ICommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class CmdDemote implements ICommand {

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("demote");
    }

    @Override
    public String getPermission() {
        return "superior.island.demote";
    }

    @Override
    public String getUsage() {
        return "island demote <player-name>";
    }

    @Override
    public String getDescription() {
        return Locale.COMMAND_DESCRIPTION_DEMOTE.getMessage();
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public boolean canBeExecutedByConsole() {
        return false;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        SuperiorPlayer superiorPlayer = SSuperiorPlayer.of(sender);
        Island island = superiorPlayer.getIsland();

        if(island == null){
            Locale.INVALID_ISLAND.send(superiorPlayer);
            return;
        }

        if(!superiorPlayer.hasPermission(IslandPermission.DEMOTE_MEMBERS)){
            Locale.NO_DEMOTE_PERMISSION.send(superiorPlayer, island.getRequiredRole(IslandPermission.DEMOTE_MEMBERS));
            return;
        }

        SuperiorPlayer targetPlayer = SSuperiorPlayer.of(args[1]);

        if(targetPlayer == null){
            Locale.INVALID_PLAYER.send(superiorPlayer, args[1]);
            return;
        }

        if(!targetPlayer.getIslandRole().isLessThan(superiorPlayer.getIslandRole())){
            Locale.DEMOTE_PLAYERS_WITH_LOWER_ROLE.send(superiorPlayer);
            return;
        }

        if(targetPlayer.getIslandRole().getPreviousRole() == IslandRole.GUEST){
            Locale.LAST_ROLE_DEMOTE.send(superiorPlayer);
            return;
        }

        targetPlayer.setIslandRole(targetPlayer.getIslandRole().getPreviousRole());

        Locale.DEMOTED_MEMBER.send(superiorPlayer, targetPlayer.getName(), targetPlayer.getIslandRole());
        Locale.GOT_DEMOTED.send(targetPlayer, targetPlayer.getIslandRole());
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        SuperiorPlayer superiorPlayer = SSuperiorPlayer.of(sender);
        Island island = superiorPlayer.getIsland();

        if(args.length == 2 && island != null && superiorPlayer.hasPermission(IslandPermission.DEMOTE_MEMBERS)){
            List<String> list = new ArrayList<>();
            SuperiorPlayer targetPlayer;

            for(UUID uuid : island.getAllMembers()){
                targetPlayer = SSuperiorPlayer.of(uuid);
                if(targetPlayer.getIslandRole().isLessThan(superiorPlayer.getIslandRole()) &&
                        targetPlayer.getIslandRole().getPreviousRole() != IslandRole.GUEST &&
                        targetPlayer.getName().toLowerCase().startsWith(args[1].toLowerCase())){
                    list.add(targetPlayer.getName());
                }
            }

            return list;
        }

        return new ArrayList<>();
    }
}
