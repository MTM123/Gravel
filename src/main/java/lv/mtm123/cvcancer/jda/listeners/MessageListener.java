package lv.mtm123.cvcancer.jda.listeners;

import com.earth2me.essentials.User;
import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.config.Config;
import lv.mtm123.cvcancer.jda.MarkdownConverter;
import lv.mtm123.cvcancer.players.DiscordPlayer;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MessageListener extends ListenerAdapter {

    private static Runnable updatePlayers;
    private final CVCancer plugin;
    private final Config config;
    private final MarkdownConverter converter;

    public MessageListener(CVCancer plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.converter = new MarkdownConverter();
        updatePlayers = this::reconstructDiscordPlayers;
        reconstructDiscordPlayers();
    }

    public static Runnable getUpdatePlayersRunnable() {
        return updatePlayers;
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        super.onGuildMemberJoin(event);
        //Update our custom users
        reconstructDiscordPlayers();
    }

    @Override
    public void onGuildMemberLeave(@Nonnull GuildMemberLeaveEvent event) {
        super.onGuildMemberLeave(event);
        //Update our custom users
        reconstructDiscordPlayers();
    }

    private void reconstructDiscordPlayers() {
        assert plugin.getJda() != null;
        TextChannel link = plugin.getJda().getTextChannelById(config.getChatLinkChannel());

        if (link != null) {
            ArrayList<DiscordPlayer> players =
                    link.getMembers().stream()
                            .filter(m -> !m.getUser().isBot())
                            .filter(m -> !config.getChatLinkMentionExclusions().contains(m.getIdLong()))
                            .map(DiscordPlayer::new)
                            .collect(Collectors.toCollection(ArrayList::new));

            plugin.getEssentials().getCustomPlayers().clear();
            for (DiscordPlayer essPlayer : players) {
                plugin.getEssentials().addCustomPlayer(essPlayer);
                User user = plugin.getEssentials().getUser(essPlayer);
                user.setNPC(true);
                user.setTeleportEnabled(false);
                user.setAutoTeleportEnabled(false);
            }
        }
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {

        if (event.getChannel().getIdLong() != config.getChatLinkChannel()
                || event.getMessage().getAuthor().isBot()) {
            return;
        }

        String name = event.getMember() == null
                ? event.getAuthor().getName()
                : event.getMember().getEffectiveName();
        String message = ChatColor.translateAlternateColorCodes('&',
                converter.compute(event.getMessage().getContentDisplay()));

        Bukkit.getScheduler().runTask(plugin,
                () -> {
                    BaseComponent[] msg = new ComponentBuilder("<" +
                            ChatColor.GRAY + "@" + ChatColor.RESET + name + ">")
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new BaseComponent[]{new TextComponent(
                                            ChatColor.GRAY + "Click here to mention this user")}))
                            .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "@" + name + " "))
                            .append(" ")
                            .append(TextComponent.fromLegacyText(message), ComponentBuilder.FormatRetention.NONE)
                            .create();
                    Bukkit.broadcast(msg);
                });
    }

}
