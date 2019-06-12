package com.bgsoftware.superiorskyblock.commands.command;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.menu.IslandMembersMenu;
import com.bgsoftware.superiorskyblock.menu.IslandPanelMenu;
import com.bgsoftware.superiorskyblock.menu.IslandVisitorsMenu;
import com.bgsoftware.superiorskyblock.wrappers.SSuperiorPlayer;
import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.commands.ICommand;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CmdPanel implements ICommand {

    @Override
    public List<String> getAliases() {
        return Arrays.asList("panel", "manager", "cp");
    }

    @Override
    public String getPermission() {
        return "superior.island.panel";
    }

    @Override
    public String getUsage() {
        return "island panel [members/visitors] [toggle]";
    }

    @Override
    public int getMinArgs() {
        return 1;
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

        if(superiorPlayer.getIsland() == null){
            Locale.INVALID_ISLAND.send(superiorPlayer);
            return;
        }

        if(args.length > 1){
            if(args[1].equalsIgnoreCase("members")){
                IslandMembersMenu.openInventory(superiorPlayer, null, superiorPlayer.getIsland());
                return;
            }else if(args[1].equalsIgnoreCase("visitors")){
                IslandVisitorsMenu.openInventory(superiorPlayer, null, superiorPlayer.getIsland());
                return;
            }else if(args[1].equalsIgnoreCase("toggle")){
                if(superiorPlayer.hasToggledPanel()){
                    superiorPlayer.setToggledPanel(false);
                    Locale.PANEL_TOGGLE_OFF.send(superiorPlayer);
                }else{
                    superiorPlayer.setToggledPanel(true);
                    Locale.PANEL_TOGGLE_ON.send(superiorPlayer);
                }
                return;
            }
        }

        IslandPanelMenu.openInventory(superiorPlayer, null);
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        return new ArrayList<>();
    }
}
