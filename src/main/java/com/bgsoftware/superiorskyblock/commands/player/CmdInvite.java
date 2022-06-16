package com.bgsoftware.superiorskyblock.commands.player;

import com.bgsoftware.superiorskyblock.SuperiorSkyblockPlugin;
import com.bgsoftware.superiorskyblock.api.island.Island;
import com.bgsoftware.superiorskyblock.api.island.IslandPrivilege;
import com.bgsoftware.superiorskyblock.api.service.message.IMessageComponent;
import com.bgsoftware.superiorskyblock.api.wrappers.SuperiorPlayer;
import com.bgsoftware.superiorskyblock.commands.CommandTabCompletes;
import com.bgsoftware.superiorskyblock.commands.IPermissibleCommand;
import com.bgsoftware.superiorskyblock.commands.arguments.CommandArguments;
import com.bgsoftware.superiorskyblock.island.permissions.IslandPrivileges;
import com.bgsoftware.superiorskyblock.lang.Message;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class CmdInvite implements IPermissibleCommand {

    @Override
    public List<String> getAliases() {
        return Arrays.asList("invite", "add");
    }

    @Override
    public String getPermission() {
        return "superior.island.invite";
    }

    @Override
    public String getUsage(java.util.Locale locale) {
        return "invite <" + Message.COMMAND_ARGUMENT_PLAYER_NAME.getMessage(locale) + ">";
    }

    @Override
    public String getDescription(java.util.Locale locale) {
        return Message.COMMAND_DESCRIPTION_INVITE.getMessage(locale);
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
    public IslandPrivilege getPrivilege() {
        return IslandPrivileges.INVITE_MEMBER;
    }

    @Override
    public Message getPermissionLackMessage() {
        return Message.NO_INVITE_PERMISSION;
    }

    @Override
    public void execute(SuperiorSkyblockPlugin plugin, SuperiorPlayer superiorPlayer, Island island, String[] args) {
        SuperiorPlayer targetPlayer = CommandArguments.getPlayer(plugin, superiorPlayer, args[1]);

        if (targetPlayer == null)
            return;

        if (island.isMember(targetPlayer)) {
            Message.ALREADY_IN_ISLAND_OTHER.send(superiorPlayer);
            return;
        }

        if (island.isBanned(targetPlayer)) {
            Message.INVITE_BANNED_PLAYER.send(superiorPlayer);
            return;
        }

        boolean isTargetOnline = targetPlayer.isOnline();

        java.util.Locale locale = superiorPlayer.getUserLocale();
        IMessageComponent messageComponent;

        if (island.isInvited(targetPlayer)) {
            island.revokeInvite(targetPlayer);
            messageComponent = Message.REVOKE_INVITE_ANNOUNCEMENT.getComponent(locale);
            if (isTargetOnline)
                Message.GOT_REVOKED.send(targetPlayer, superiorPlayer.getName());
        } else {
            if (island.getTeamLimit() >= 0 && island.getIslandMembers(true).size() >= island.getTeamLimit()) {
                Message.INVITE_TO_FULL_ISLAND.send(superiorPlayer);
                return;
            }

            if (!plugin.getEventsBus().callIslandInviteEvent(superiorPlayer, targetPlayer, island))
                return;

            island.inviteMember(targetPlayer);
            messageComponent = Message.INVITE_ANNOUNCEMENT.getComponent(locale);

            if (isTargetOnline)
                Message.GOT_INVITE.send(targetPlayer, superiorPlayer.getName());
        }

        if (messageComponent != null)
            island.sendMessage(messageComponent, superiorPlayer.getName(), targetPlayer.getName());
    }

    @Override
    public List<String> tabComplete(SuperiorSkyblockPlugin plugin, SuperiorPlayer superiorPlayer, Island island, String[] args) {
        return args.length == 2 ? CommandTabCompletes.getOnlinePlayers(plugin, args[1],
                plugin.getSettings().isTabCompleteHideVanished(), onlinePlayer -> !island.isMember(onlinePlayer)) :
                new ArrayList<>();
    }

}
