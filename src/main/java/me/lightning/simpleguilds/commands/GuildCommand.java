package me.lightning.simpleguilds.commands;

import com.google.gson.Gson;
import me.lightning.simpleguilds.Guild;
import me.lightning.simpleguilds.GuildManager;
import me.lightning.simpleguilds.Request;
import me.lightning.simpleguilds.RequestManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuildCommand implements TabExecutor {
    private static final String PREFIX = "§6§lSimpleGuilds §7§l> " + ChatColor.RESET;
    private static final String HELPMSG =
            """
                    §7--------------------------------------------------
                    §6                   §lSimpleGuilds§r
                    §7--------------------------------------------------
                    §2/guild create <name>§r   §7- Create a new guild
                    §2/guild disband§r          §7- Disband your guild
                    §2/guild list§r            §7- List all players in your guild
                    §2/guild join <name>§r            §7- Request to join a guild
                    §2/guild invite <player>§r    §7- Invite a player to your guild
                    §2/guild kick <player>§r   §7- Kick a player from your guild
                    §2/guild leave§r           §7- Leave your guild
                    §2/guild help§r            §7- Show this message
                    §7--------------------------------------------------
                    """;
    private static final String NOGUILDMSG = "§4You’re not in a guild. Create one with /guild create <name> or join with /guild join <guild>.";
    private static final String NOTOWNER = "§4Only the guild owner can do this.";


    private static String guildList(Guild guild) {

        String owner = Bukkit.getOfflinePlayer(guild.getOwner()).getName();
        String numMembers = String.valueOf(guild.getNumMembers() - 1);
        //get all guild members
        List<String> membersList = new ArrayList<>(guild.getMembers().stream()
                .map(uuid -> Bukkit.getOfflinePlayer(uuid).getName())
                .toList());
        membersList.remove(owner);

        String members = String.join(", ", membersList);


        if (numMembers.equalsIgnoreCase("0")) {
            return "§7---------------------------------------------\n" +
                    " §6§l                  " + guild.getName() + "\n" +
                    "§7---------------------------------------------\n" +
                    "§c§lOwner§7: §f"+owner+"\n" +
                    "§7---------------------------------------------";
        } else {
            return
                    "§7---------------------------------------------\n" +
                    " §6                       §l" + guild.getName()+"\n" +
                    "§7---------------------------------------------\n" +
                    "§c§lOwner§7: §f"+owner+"\n" +
                    "§2§lMembers§7:§f "+members+"\n" +
                    "§7---------------------------------------------";
        }
    }
    private static final List<String> GUILD_SUBCOMMANDS = List.of(
            "create", "disband", "list", "join", "invite",
            "accept", "deny", "kick", "leave", "help"
    );

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(commandSender instanceof Player sender)) {
            commandSender.sendMessage("Only players can execute this command");
            return true;
        }
        if ((args.length < 1)) {
            sender.sendMessage(HELPMSG);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                //player didn't enter the guild name
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "§4Usage: /guild create <name>");
                    return true;
                }
                //player already have a guild
                if (GuildManager.getPlayerGuild(sender) != null) {
                    sender.sendMessage(PREFIX + "§4You’re already part of a guild. Leave it before creating another.");
                    return true;
                }
                //Create the guild
                String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                GuildManager.GuildCreateResult result = GuildManager.createGuild(guildName,sender.getUniqueId());

                switch (result) {
                    case NAME_TOO_SHORT -> sender.sendMessage(PREFIX + "§4Guild name must be at least 3 characters long.");
                    case NAME_TOO_LONG -> sender.sendMessage(PREFIX + "§4Guild name cannot be longer than 30 characters.");
                    case NAME_TAKEN -> sender.sendMessage(PREFIX + "§4A guild with that name already exists.");
                    case SUCCESS -> sender.sendMessage(PREFIX + "§2Your guild has been created successfully!");
                }
                return true;
            }
            case "disband" -> {
                //check if player has a guild
                Guild guild = GuildManager.getPlayerGuild(sender);
                if (guild == null) {
                    sender.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }
                //check if player is the owner of the guild
                if (!guild.getOwner().equals(sender.getUniqueId())) {
                    sender.sendMessage(PREFIX + NOTOWNER);
                    return true;
                }
                //Delete the guild
                GuildManager.deleteGuild(guild);
                sender.sendMessage(PREFIX + "§2The guild has been disbanded successfully.");
                guild.broadcast(PREFIX + "§7Your guild has been disbanded.", sender);
                return true;

            }
            case "list"   -> {
                //check if player have a guild
                if (GuildManager.getPlayerGuild(sender) == null) {
                    sender.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }
                sender.sendMessage(guildList(GuildManager.getPlayerGuild(sender)));
                return true;

            }
            case "join" -> {
                //player didn't enter the guild name
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "§4Please enter the guild name you want to join.");
                    return true;
                }
                //player already have a guild
                if (GuildManager.getPlayerGuild(sender) != null) {
                    sender.sendMessage(PREFIX + "§4You’re already part of a guild. Leave it before joining another.");
                    return true;
                }
                //check if that guild exists
                String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Guild guild = GuildManager.getGuild(guildName);
                if (guild == null) {
                    sender.sendMessage(PREFIX + "§4That guild doesn’t exist.");
                    return true;
                }
                //send the guild owner a request to join the guild
                Player owner = Bukkit.getPlayer(guild.getOwner());
                if (owner != null) {
                    Request request = new Request(sender.getUniqueId(),owner.getUniqueId(), Request.RequestType.JOIN,guild.getName());
                    RequestManager.addRequest(request);

                    sender.sendMessage(PREFIX + "§7Your request to join " + "§e" + guild.getName() + " §7has been sent.");
                    Component message = Component.text(PREFIX + "§e" + sender.getName() + " §7wants to join your guild! ")
                                    .append(Component.text("§a[Accept]").clickEvent(ClickEvent.runCommand("/guild accept " + sender.getName()))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to accept")))
                                    .append(Component.text(" §7| ")
                                    .append(Component.text("§c[Deny]")
                                    .clickEvent(ClickEvent.runCommand("/guild deny " + sender.getName()))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to deny"))))));

                    owner.sendMessage(message);
                }


            }
            case "invite" -> {
                //didn't enter the player ign
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "§4Please enter the player name you want to invite.");
                    return true;
                }
                //make sure this player is online
                String playerIgn = args[1];
                Player invitedPlayer = Bukkit.getPlayer(playerIgn);

                if (invitedPlayer == null) {
                    sender.sendMessage(PREFIX + "§4You can’t invite this player while they’re offline.");
                    return true;
                }
                //invited player already have a guild
                if (GuildManager.getPlayerGuild(invitedPlayer) != null) {
                    sender.sendMessage(PREFIX + "§4This player is already part of a guild.");
                    return true;
                }
                //make sure the sender have a guild
                if (GuildManager.getPlayerGuild(sender) == null) {
                    sender.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }
                Guild guild = GuildManager.getPlayerGuild(sender);
                //make sure the sender is the owner of the guild
                if (!(guild.getOwner().equals(sender.getUniqueId()))) {
                    sender.sendMessage(PREFIX + NOTOWNER);
                    return true;
                }
                //send the invitedPlayer a request to join the guild, and they can accept or deny by clicking the msg
                Request request = new Request(sender.getUniqueId(),invitedPlayer.getUniqueId(), Request.RequestType.INVITE,guild.getName());
                RequestManager.addRequest(request);

                sender.sendMessage(PREFIX + "§e" + invitedPlayer.getName() + " §7has been invited to join your guild.");
                Component messege = Component.text(PREFIX + "§r§7You have been invited to join the guild " + "§e" + guild.getName() + " ")
                                                .append(Component.text("§a[Accept]").clickEvent(ClickEvent.runCommand("/guild accept"))
                                                .hoverEvent(HoverEvent.showText(Component.text("Click to accept")))
                                                .append(Component.text(" §7| ")
                                                .append(Component.text("§c[Deny]")
                                                .clickEvent(ClickEvent.runCommand("/guild deny"))
                                                .hoverEvent(HoverEvent.showText(Component.text("Click to deny"))))));
                invitedPlayer.sendMessage(messege);


            }
            case "accept" -> {
                //check if the player have any ongoing requests
                Request request = RequestManager.getRequest(sender.getUniqueId());
                if (request == null) {
                    sender.sendMessage(PREFIX + "§4There are no pending invite or join requests");
                    return true;
                }
                //invite request
                if (args.length == 1 && request.getType() == Request.RequestType.INVITE) {
                    //player already have a guild
                    if (GuildManager.getPlayerGuild(sender) != null) {
                        sender.sendMessage(PREFIX + "§4You are already part of a guild.");
                        return true;
                    }
                    //check if guild still exists
                    if (GuildManager.getGuild(request.getGuildName()) == null) {
                        sender.sendMessage(PREFIX + "§4That guild no longer exists.");
                        return true;
                    }

                    if (Bukkit.getPlayer(request.getSender()) != null) {
                        Bukkit.getPlayer(request.getSender()).sendMessage(PREFIX + "§e" + Bukkit.getOfflinePlayer(request.getTarget()).getName() + " §7has accepted your invitation.");
                    }

                    if (Bukkit.getPlayer(request.getTarget()) != null) {
                        Bukkit.getPlayer(request.getTarget()).sendMessage(PREFIX + "§7You have joined " + "§e" + request.getGuildName());
                    }

                    //Add the player
                    Guild guild = GuildManager.getGuild(request.getGuildName());
                    RequestManager.removeRequest(sender.getUniqueId());
                    guild.addMember(request.getTarget());
                    guild.broadcast(PREFIX + "§e" + Bukkit.getOfflinePlayer(request.getTarget()).getName() + " §7has joined the guild.", Bukkit.getPlayer(request.getTarget()));
                    return true;
                }
                if (args.length == 2 && request.getType() == Request.RequestType.JOIN) {

                    String senderIGN = args[1];

                    //make sure it's the right player
                    if (!(senderIGN.equalsIgnoreCase(Bukkit.getOfflinePlayer(request.getSender()).getName()))) {
                        sender.sendMessage(PREFIX + "§4That player has not requested to join your guild");
                        return true;
                    }
                    //Guild check
                    if (GuildManager.getPlayerGuild(Bukkit.getPlayer(request.getSender())) != null) {
                        sender.sendMessage(PREFIX + "§4That player has already joined another guild.");
                        return true;
                    }
                    //make sure that guild still exists
                    if (GuildManager.getGuild(request.getGuildName()) == null) {
                        sender.sendMessage(PREFIX + "§4That guild no longer exists.");
                        return true;
                    }
                    //make sure the player is the owner of the guild
                    if (!(GuildManager.getGuild(request.getGuildName()).getOwner().equals(sender.getUniqueId()))) {
                        sender.sendMessage(PREFIX + NOTOWNER);
                        return true;
                    }
                    if (Bukkit.getPlayer(request.getSender()) != null) {
                        Bukkit.getPlayer(request.getSender()).sendMessage(PREFIX + "§7You have joined " + "§e" + request.getGuildName());
                    }

                    if (Bukkit.getPlayer(request.getTarget()) != null) {
                        Bukkit.getPlayer(request.getTarget()).sendMessage(PREFIX + "§2You have accepted the join request.");
                    }
                    //add that player to the guild
                    Guild guild = GuildManager.getGuild(request.getGuildName());
                    RequestManager.removeRequest(sender.getUniqueId());
                    guild.addMember(request.getSender());
                    guild.broadcast(PREFIX + "§e" + Bukkit.getOfflinePlayer(request.getSender()).getName() + " §7has joined the guild.",Bukkit.getPlayer(request.getSender()));
                    return true;
                }
                //invalid usage
                if (args.length < 2 && request.getType() == Request.RequestType.JOIN) {
                    sender.sendMessage(PREFIX + "§4Please enter the name of the player you want to accept.");
                    return true;
                }
                sender.sendMessage(HELPMSG);
                return true;
            }
            case "deny" -> {
                //check if the player have any ongoing requests
                Request request = RequestManager.getRequest(sender.getUniqueId());
                if (request == null) {
                    sender.sendMessage(PREFIX + "§4There are no pending invite or join requests");
                    return true;
                }
                //invite request
                if (args.length == 1 && request.getType() == Request.RequestType.INVITE) {

                    if (Bukkit.getPlayer(request.getTarget()) != null) {
                        Bukkit.getPlayer(request.getTarget()).sendMessage(PREFIX + "§7You have denied the invite request.");
                    }

                    //remove the request
                    RequestManager.removeRequest(sender.getUniqueId());
                    return true;
                }
                if (args.length == 2 && request.getType() == Request.RequestType.JOIN) {

                    String senderIGN = args[1].toLowerCase();

                    //make sure it's the right player
                    if (!(senderIGN.equalsIgnoreCase(Bukkit.getOfflinePlayer(request.getSender()).getName().toLowerCase()))) {
                        sender.sendMessage(PREFIX + "§4That player has not requested to join your guild");
                        return true;
                    }

                    //make sure the player is the owner of the guild
                    if (!(GuildManager.getGuild(request.getGuildName()).getOwner().equals(sender.getUniqueId()))) {
                        sender.sendMessage(PREFIX + NOTOWNER);
                        return true;
                    }

                    if (Bukkit.getPlayer(request.getTarget()) != null) {
                        Bukkit.getPlayer(request.getTarget()).sendMessage(PREFIX + "§7You have denied the join request.");
                    }
                    //remove the request
                    RequestManager.removeRequest(sender.getUniqueId());
                    return true;
                }
                //invalid usage
                if (args.length < 2 && request.getType() == Request.RequestType.JOIN) {
                    sender.sendMessage(PREFIX + "§4Please enter the name of the player you want to deny.");
                    return true;
                }
                sender.sendMessage(HELPMSG);
                return true;
            }
            case "kick" -> {
                //didnt provide the player to kick
                if (args.length < 2) {
                    sender.sendMessage(PREFIX + "§4Please enter the name of the player you want to kick.");
                    return true;
                }
                OfflinePlayer player = Bukkit.getOfflinePlayer(args[1]);
                //dont have a guild
                Guild guild = GuildManager.getPlayerGuild(sender);
                if (guild == null) {
                    sender.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }
                //not the owner of the guild
                if (!(guild.getOwner().equals(sender.getUniqueId()))) {
                    sender.sendMessage(PREFIX + NOTOWNER);
                    return true;
                }
                //not in the guild
                if (!(guild.hasMember(player.getUniqueId()))) {
                    sender.sendMessage(PREFIX + "§4This player isn’t in your guild.");
                    return true;
                }
                //kicking the guild owner
                if (player.getUniqueId().equals(guild.getOwner())) {
                    sender.sendMessage(PREFIX + "§4You can’t kick yourself.");
                    return true;
                }
                //kick the player
                if (player.isOnline()) {
                    Player onlinePlayer = player.getPlayer();
                    onlinePlayer.sendMessage(PREFIX + "§7You have been kicked from §e" + guild.getName());
                }
                guild.removeMember(player.getUniqueId());
                sender.sendMessage(PREFIX + "§7You have kicked §e" + player.getName());
                guild.broadcast(PREFIX + "§e" + player.getName() + " §7has been kicked from the guild.", sender);
                return true;

            }
            case "leave" -> {
                //no guild
                Guild guild = GuildManager.getPlayerGuild(sender);
                if (guild == null) {
                    sender.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }
                //delete if the owner left the guild
                if (guild.getOwner().equals(sender.getUniqueId())) {
                    GuildManager.deleteGuild(guild);
                    guild.broadcast(PREFIX + "§7Your guild has been disbanded.", sender);
                    sender.sendMessage(PREFIX + "§7You were the guild owner — the guild has been disbanded.");
                    return true;
                }
                //Remove the player
                guild.removeMember(sender.getUniqueId());
                sender.sendMessage(PREFIX + "§7You have left the guild.");
                guild.broadcast(PREFIX + "§e" + sender.getName() + " §7has left the guild.");
                return true;
            }
            default -> sender.sendMessage(HELPMSG);
        }
        return true;
    }




    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender,
                                                @NotNull Command command,
                                                @NotNull String label,
                                                @NotNull String[] args) {
        final List<String> validArgs = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0],GUILD_SUBCOMMANDS,validArgs);
            return validArgs;
        }
        return List.of();
    }
}
