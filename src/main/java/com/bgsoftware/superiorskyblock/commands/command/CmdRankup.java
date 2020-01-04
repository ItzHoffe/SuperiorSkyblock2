package com.bgsoftware.superiorskyblock.commands.command;

import com.bgsoftware.superiorskyblock.Locale;
import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.events.IslandUpgradeEvent;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPermission;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.commands.ICommand;
import com.bgsoftware.superiorskyblock.hooks.EconomyHook;
import com.bgsoftware.superiorskyblock.wrappers.SSuperiorPlayer;
import com.bgsoftware.superiorskyblock.wrappers.SoundWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CmdRankup implements ICommand {

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("rankup");
    }

    @Override
    public String getPermission() {
        return "superior.island.rankup";
    }

    @Override
    public String getUsage(java.util.Locale locale) {
        return "rankup <" + Locale.COMMAND_ARGUMENT_UPGRADE_NAME.getMessage(locale) + ">";
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return Locale.COMMAND_DESCRIPTION_RANKUP.getMessage(locale);
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

        if(!superiorPlayer.hasPermission(IslandPermission.RANKUP)){
            Locale.NO_RANKUP_PERMISSION.send(superiorPlayer, island.getRequiredPlayerRole(IslandPermission.RANKUP));
            return;
        }

        String upgradeName = args[1];

        if(!plugin.getUpgrades().isUpgrade(upgradeName)){
            Locale.INVALID_UPGRADE.send(superiorPlayer, upgradeName, getUpgradesString(plugin));
            return;
        }

        int level = island.getUpgradeLevel(upgradeName);

        String permission = plugin.getUpgrades().getUpgradePermission(upgradeName, level + 1);

        if(!permission.isEmpty() && !superiorPlayer.hasPermission(permission)){
            Locale.NO_UPGRADE_PERMISSION.send(superiorPlayer);
            return;
        }

        boolean hasNextLevel;

        List<String> commands = plugin.getUpgrades().getUpgradeCommands(upgradeName, level);

        IslandUpgradeEvent islandUpgradeEvent = new IslandUpgradeEvent(superiorPlayer, island, upgradeName, commands,
                plugin.getUpgrades().getUpgradePrice(upgradeName, level));
        Bukkit.getPluginManager().callEvent(islandUpgradeEvent);

        double nextUpgradePrice = islandUpgradeEvent.getAmountToWithdraw();

        if(islandUpgradeEvent.isCancelled()){
            hasNextLevel = false;
        }

        else if(EconomyHook.getMoneyInBank(superiorPlayer) < nextUpgradePrice){
            Locale.NOT_ENOUGH_MONEY_TO_UPGRADE.send(superiorPlayer);
            hasNextLevel = false;
        }

        else {
            if (nextUpgradePrice > 0)
                EconomyHook.withdrawMoney(superiorPlayer, nextUpgradePrice);

            for (String command : commands) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", superiorPlayer.getName()));
            }

            hasNextLevel = true;
        }

        SoundWrapper sound = plugin.getUpgrades().getClickSound(upgradeName, level, hasNextLevel);
        if(sound != null)
            sound.playSound(superiorPlayer.asPlayer());
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, CommandSender sender, String[] args) {
        SuperiorPlayer superiorPlayer = SSuperiorPlayer.of(sender);
        Island island = superiorPlayer.getIsland();

        if(args.length == 2 && island != null && superiorPlayer.hasPermission(IslandPermission.RANKUP)){
            List<String> list = new ArrayList<>();

            for(String upgrade : plugin.getUpgrades().getAllUpgrades()){
                if(upgrade.toLowerCase().startsWith(args[1].toLowerCase()))
                    list.add(upgrade.toLowerCase());
            }

            return list;
        }

        return new ArrayList<>();
    }

    private String getUpgradesString(SuperiorSkyblockPlugin plugin){
        StringBuilder stringBuilder = new StringBuilder();

        for(String upgrade : plugin.getUpgrades().getAllUpgrades())
            stringBuilder.append(", ").append(upgrade);

        return stringBuilder.toString().substring(2);
    }

}
