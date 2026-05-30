package me.lightning.simpleguilds.commands;

import me.lightning.simpleguilds.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
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

import java.util.*;

public class GuildCommand implements TabExecutor {
    private static final String PREFIX = "§6§lSimpleGuilds §7§l> " + ChatColor.RESET;

    private static final String HELPMSG =
            """
            §8§m§l-----------------------------------
            §6                   §lSimpleGuilds
            §8§m§l-----------------------------------
            §2/guild create <name>       §7- §fCreate a new guild
            §2/guild disband             §7- §fDisband your guild
            §2/guild list                §7- §fList all players in your guild
            §2/guild join <name>         §7- §fRequest to join a guild
            §2/guild invite <player>     §7- §fInvite a player to your guild
            §2/guild kick <player>       §7- §fKick a player from your guild
            §2/guild leave               §7- §fLeave your guild
            §2/guild help                §7- §fShow this message
            §2/guild motd edit <message>  §7- §fEdit the MOTD (\\n for new line)
            §2/guild motd apply          §7- §fApply the MOTD to your guild
            §8§m§l-----------------------------------
            """;

    private static final String NOGUILDMSG = "§cYou’re not in a guild. Create one with /guild create <name> or join with /guild join <guild>.";

    private static final String NOTOWNER = "§cOnly the guild owner can do this.";

    private static final Map<String, String> unsavedMOTD = new HashMap<>();

    Cooldown motdCooldown = new Cooldown();

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(commandSender instanceof Player player)) {
            commandSender.sendMessage("Only players can execute this command");
            return true;
        }
        if ((args.length < 1)) {
            player.sendMessage(HELPMSG);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create" -> {
                //player didn't enter the guild name
                if (args.length < 2) {
                    player.sendMessage(PREFIX + "§4Usage: /guild create <name>");
                    return true;
                }
                //player already have a guild
                if (GuildManager.getPlayerGuild(player) != null) {
                    player.sendMessage(PREFIX + "§cYou’re already part of a guild. Leave it before creating another.");
                    return true;
                }
                //create the guild
                String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                GuildManager.GuildCreateResult result = GuildManager.createGuild(guildName,player.getUniqueId());

                switch (result) {
                    case NAME_TOO_SHORT -> player.sendMessage(PREFIX + "§cGuild name must be at least 3 characters long.");
                    case NAME_TOO_LONG -> player.sendMessage(PREFIX + "§cGuild name cannot be longer than 30 characters.");
                    case NAME_TAKEN -> player.sendMessage(PREFIX + "§cA guild with that name already exists.");
                    case SUCCESS -> player.sendMessage(PREFIX + "§2Your guild has been created successfully!");
                }
                return true;
            }

            case "disband" -> {
                //check if player has a guild
                Guild guild = GuildManager.getPlayerGuild(player);
                if (guild == null) {
                    player.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }
                //check if player is the owner of the guild
                if (!guild.getOwner().equals(player.getUniqueId())) {
                    player.sendMessage(PREFIX + NOTOWNER);
                    return true;
                }
                //Delete the guild
                GuildManager.deleteGuild(guild);
                player.sendMessage(PREFIX + "§2The guild has been disbanded successfully.");
                guild.broadcast(PREFIX + "§7Your guild has been disbanded.", player);
                return true;

            }

            case "list"   -> {
                Guild guild = GuildManager.getPlayerGuild(player);

                //check if player have a guild
                if (guild == null) {
                    player.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }

                player.sendMessage(GuildManager.guildList(guild));
                return true;

            }

            case "join" -> {
                //player didn't enter the guild name
                if (args.length < 2) {
                    player.sendMessage(PREFIX + "§cPlease enter the guild name you want to join.");
                    return true;
                }
                //player already have a guild
                if (GuildManager.getPlayerGuild(player) != null) {
                    player.sendMessage(PREFIX + "§cYou’re already part of a guild. Leave it before joining another.");
                    return true;
                }
                //check if that guild exists
                String guildName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                Guild guild = GuildManager.getGuild(guildName);
                if (guild == null) {
                    player.sendMessage(PREFIX + "§cThat guild doesn’t exist.");
                    return true;
                }
                //send the guild owner a request to join the guild
                Player owner = Bukkit.getPlayer(guild.getOwner());
                if (owner != null) {
                    Request request = new Request(player.getUniqueId(),owner.getUniqueId(), Request.RequestType.JOIN,guild.getName());
                    RequestManager.addRequest(request);

                    player.sendMessage(PREFIX + "§7Your request to join " + "§e" + guild.getName() + " §7has been sent.");
                    TextComponent message = Component.text(PREFIX + "§e" + player.getName() + " §7wants to join your guild! ")
                                    .append(Component.text("§a[Accept]").clickEvent(ClickEvent.runCommand("/guild accept " + player.getName()))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to accept")))
                                    .append(Component.text(" §7| ")
                                    .append(Component.text("§c[Deny]")
                                    .clickEvent(ClickEvent.runCommand("/guild deny " + player.getName()))
                                    .hoverEvent(HoverEvent.showText(Component.text("Click to deny"))))));

                    SimpleGuilds.adventure().player(owner).sendMessage(message);
                }


            }

            case "invite" -> {
                //didn't enter the player ign
                if (args.length < 2) {
                    player.sendMessage(PREFIX + "§cPlease enter the player name you want to invite.");
                    return true;
                }
                //make sure this player is online
                String playerIgn = args[1];
                Player invitedPlayer = Bukkit.getPlayer(playerIgn);

                if (invitedPlayer == null) {
                    player.sendMessage(PREFIX + "§cYou can’t invite this player while they’re offline.");
                    return true;
                }
                //invited player already have a guild
                if (GuildManager.getPlayerGuild(invitedPlayer) != null) {
                    player.sendMessage(PREFIX + "§cThis player is already part of a guild.");
                    return true;
                }
                //make sure the sender have a guild
                if (GuildManager.getPlayerGuild(player) == null) {
                    player.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }
                Guild guild = GuildManager.getPlayerGuild(player);
                //make sure the sender is the owner of the guild
                if (!(guild.getOwner().equals(player.getUniqueId()))) {
                    player.sendMessage(PREFIX + NOTOWNER);
                    return true;
                }
                //send the invitedPlayer a request to join the guild, and they can accept or deny by clicking the msg
                Request request = new Request(player.getUniqueId(),invitedPlayer.getUniqueId(), Request.RequestType.INVITE,guild.getName());
                RequestManager.addRequest(request);

                player.sendMessage(PREFIX + "§e" + invitedPlayer.getName() + " §7has been invited to join your guild.");
                TextComponent message = Component.text(PREFIX + "§r§7You have been invited to join the guild " + "§e" + guild.getName() + " ")
                                                .append(Component.text("§a[Accept]").clickEvent(ClickEvent.runCommand("/guild accept"))
                                                .hoverEvent(HoverEvent.showText(Component.text("Click to accept")))
                                                .append(Component.text(" §7| ")
                                                .append(Component.text("§c[Deny]")
                                                .clickEvent(ClickEvent.runCommand("/guild deny"))
                                                .hoverEvent(HoverEvent.showText(Component.text("Click to deny"))))));
                SimpleGuilds.adventure().player(invitedPlayer).sendMessage(message);


            }

            case "accept" -> {
                //check if the player have any ongoing requests
                Request request = RequestManager.getRequest(player.getUniqueId());
                if (request == null) {
                    player.sendMessage(PREFIX + "§cThere are no pending invite or join requests");
                    return true;
                }
                //invite request
                if (args.length == 1 && request.getType() == Request.RequestType.INVITE) {
                    //player already have a guild
                    if (GuildManager.getPlayerGuild(player) != null) {
                        player.sendMessage(PREFIX + "§cYou are already part of a guild.");
                        return true;
                    }
                    //check if guild still exists
                    if (GuildManager.getGuild(request.getGuildName()) == null) {
                        player.sendMessage(PREFIX + "§cThat guild no longer exists.");
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
                    RequestManager.removeRequest(player.getUniqueId());
                    guild.addMember(request.getTarget());
                    guild.broadcast(PREFIX + "§e" + Bukkit.getOfflinePlayer(request.getTarget()).getName() + " §7has joined the guild.", Bukkit.getPlayer(request.getTarget()));
                    return true;
                }
                if (args.length == 2 && request.getType() == Request.RequestType.JOIN) {

                    String senderIGN = args[1];

                    //make sure it's the right player
                    if (!(senderIGN.equalsIgnoreCase(Bukkit.getOfflinePlayer(request.getSender()).getName()))) {
                        player.sendMessage(PREFIX + "§cThat player has not requested to join your guild");
                        return true;
                    }
                    //Guild check
                    if (GuildManager.getPlayerGuild(Bukkit.getPlayer(request.getSender())) != null) {
                        player.sendMessage(PREFIX + "§cThat player has already joined another guild.");
                        return true;
                    }
                    //make sure that guild still exists
                    if (GuildManager.getGuild(request.getGuildName()) == null) {
                        player.sendMessage(PREFIX + "§cThat guild no longer exists.");
                        return true;
                    }
                    //make sure the player is the owner of the guild
                    if (!(GuildManager.getGuild(request.getGuildName()).getOwner().equals(player.getUniqueId()))) {
                        player.sendMessage(PREFIX + NOTOWNER);
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
                    RequestManager.removeRequest(player.getUniqueId());
                    guild.addMember(request.getSender());
                    guild.broadcast(PREFIX + "§e" + Bukkit.getOfflinePlayer(request.getSender()).getName() + " §7has joined the guild.",Bukkit.getPlayer(request.getSender()));
                    return true;
                }
                //invalid usage
                if (args.length < 2 && request.getType() == Request.RequestType.JOIN) {
                    player.sendMessage(PREFIX + "§cPlease enter the name of the player you want to accept.");
                    return true;
                }
                player.sendMessage(HELPMSG);
                return true;
            }

            case "deny" -> {
                //check if the player have any ongoing requests
                Request request = RequestManager.getRequest(player.getUniqueId());
                if (request == null) {
                    player.sendMessage(PREFIX + "§cThere are no pending invite or join requests");
                    return true;
                }
                //invite request
                if (args.length == 1 && request.getType() == Request.RequestType.INVITE) {

                    if (Bukkit.getPlayer(request.getTarget()) != null) {
                        Bukkit.getPlayer(request.getTarget()).sendMessage(PREFIX + "§7You have denied the invite request.");
                    }

                    //remove the request
                    RequestManager.removeRequest(player.getUniqueId());
                    return true;
                }
                if (args.length == 2 && request.getType() == Request.RequestType.JOIN) {

                    String senderIGN = args[1].toLowerCase();

                    //make sure it's the right player
                    if (!(senderIGN.equalsIgnoreCase(Bukkit.getOfflinePlayer(request.getSender()).getName().toLowerCase()))) {
                        player.sendMessage(PREFIX + "§cThat player has not requested to join your guild");
                        return true;
                    }

                    //make sure the player is the owner of the guild
                    if (!(GuildManager.getGuild(request.getGuildName()).getOwner().equals(player.getUniqueId()))) {
                        player.sendMessage(PREFIX + NOTOWNER);
                        return true;
                    }

                    if (Bukkit.getPlayer(request.getTarget()) != null) {
                        Bukkit.getPlayer(request.getTarget()).sendMessage(PREFIX + "§7You have denied the join request.");
                    }
                    //remove the request
                    RequestManager.removeRequest(player.getUniqueId());
                    return true;
                }
                //invalid usage
                if (args.length < 2 && request.getType() == Request.RequestType.JOIN) {
                    player.sendMessage(PREFIX + "§cPlease enter the name of the player you want to deny.");
                    return true;
                }
                player.sendMessage(HELPMSG);
                return true;
            }

            case "kick" -> {
                //didnt provide the player to kick
                if (args.length < 2) {
                    player.sendMessage(PREFIX + "§cPlease enter the name of the player you want to kick.");
                    return true;
                }
                OfflinePlayer kickedPlayer = Bukkit.getOfflinePlayer(args[1]);

                //dont have a guild
                Guild guild = GuildManager.getPlayerGuild(player);
                if (guild == null) {
                    player.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }
                //not the owner of the guild
                if (!(guild.getOwner().equals(player.getUniqueId()))) {
                    player.sendMessage(PREFIX + NOTOWNER);
                    return true;
                }
                //not in the guild
                if (!(guild.hasMember(kickedPlayer.getUniqueId()))) {
                    player.sendMessage(PREFIX + "§cThis player isn’t in your guild.");
                    return true;
                }
                //kicking the guild owner
                if (kickedPlayer.getUniqueId().equals(guild.getOwner())) {
                    player.sendMessage(PREFIX + "§cYou can’t kick yourself.");
                    return true;
                }
                //kick the player
                if (kickedPlayer.isOnline()) {
                    Player onlinePlayer = kickedPlayer.getPlayer();
                    onlinePlayer.sendMessage(PREFIX + "§7You have been kicked from §e" + guild.getName());
                }
                guild.removeMember(kickedPlayer.getUniqueId());
                player.sendMessage(PREFIX + "§7You have kicked §e" + kickedPlayer.getName());
                guild.broadcast(PREFIX + "§e" + kickedPlayer.getName() + " §7has been kicked from the guild.", player);
                return true;

            }

            case "leave" -> {
                //no guild
                Guild guild = GuildManager.getPlayerGuild(player);
                if (guild == null) {
                    player.sendMessage(PREFIX + NOGUILDMSG);
                    return true;
                }
                //delete if the owner left the guild
                if (guild.getOwner().equals(player.getUniqueId())) {
                    GuildManager.deleteGuild(guild);
                    guild.broadcast(PREFIX + "§7Your guild has been disbanded.", player);
                    player.sendMessage(PREFIX + "§7You were the guild owner — the guild has been disbanded.");
                    return true;
                }
                //Remove the player
                guild.removeMember(player.getUniqueId());
                player.sendMessage(PREFIX + "§7You have left the guild.");
                guild.broadcast(PREFIX + "§e" + player.getName() + " §7has left the guild.");
                return true;
            }

            case "motd" -> {

                if ((args.length < 2)) {
                    player.sendMessage(HELPMSG);
                    return true;
                }

                String motdSubCommands = args[1].toLowerCase();
                Guild guild = GuildManager.getPlayerGuild(player);


                switch (motdSubCommands) {

                    case "edit" -> {
                        //no guild
                        if (guild == null) {
                            player.sendMessage(PREFIX + NOGUILDMSG);
                            return true;
                        }

                        //not owner
                        if (!guild.getOwner().equals(player.getUniqueId())) {
                            player.sendMessage(PREFIX + NOTOWNER);
                            return true;
                        }

                        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

                        //no message
                        if (message.isBlank()) {
                            player.sendMessage(PREFIX + "§cPlease provide a message.");
                            return true;
                        }

                        message = message.replace("\\n", "\n");
                        message = ChatColor.translateAlternateColorCodes('&', message);

                        player.sendMessage(message);
                        unsavedMOTD.put(guild.getName(), message);
                        return true;
                    }

                    case "apply" -> {
                        //no guild
                        if (guild == null) {
                            player.sendMessage(PREFIX + NOGUILDMSG);
                            return true;
                        }

                        //not owner
                        if (!guild.getOwner().equals(player.getUniqueId())) {
                            player.sendMessage(PREFIX + NOTOWNER);
                            return true;
                        }

                        String message = unsavedMOTD.get(guild.getName());
                        //no motd
                        if (message == null || message.isBlank()) {
                            player.sendMessage(PREFIX + "§cSet a MOTD first using /guild motd edit <message>.");
                            return true;
                        }

                        if (!motdCooldown.isOver(player)) {
                            player.sendMessage(PREFIX + "§cPlease wait before updating the motd again.");
                            return true;
                        }
                        motdCooldown.start(player, 30);
                        guild.setMOTD(message);
                        player.sendMessage(PREFIX + "§aGuild MOTD updated!");
                        unsavedMOTD.remove(guild.getName());
                        return true;
                    }

                    default -> player.sendMessage(HELPMSG);
                }
                return true;
            }

            default -> player.sendMessage(HELPMSG);
        }
        return true;
    }




    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        final List<String> GUILD_SUBCOMMANDS = List.of(
                "create", "disband", "list", "join", "invite",
                "accept", "deny", "kick", "leave", "help", "motd"
        );

        final List<String> validArgs = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0],GUILD_SUBCOMMANDS,validArgs);
            return validArgs;
        }
        if (args[0].equalsIgnoreCase("motd") && args.length == 2) {
            return List.of("edit", "apply");
        }
        return List.of();
    }
}
