package lv.mtm123.cvcancer.jda.listeners;

import com.earth2me.essentials.User;
import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.config.Config;
import lv.mtm123.cvcancer.jda.JdaUtils;
import lv.mtm123.cvcancer.jda.MarkdownConverter;
import lv.mtm123.cvcancer.players.DiscordPlayer;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import javax.annotation.Nonnull;
import java.util.Arrays;

public class MessageListener extends ListenerAdapter {

    private final CVCancer plugin;
    private final Config config;
    private final MarkdownConverter converter;
    private static String[] commandsToIgnore = new String[]{"msg", "message", "mention", "mentions"};

    public static boolean shouldIgnoreCommand(String message) {
        return Arrays.stream(commandsToIgnore).anyMatch(c -> message.startsWith("-" + c));
    }

    public MessageListener(CVCancer plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.converter = new MarkdownConverter();
    }

    @Override
    public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return;
        if (shouldIgnoreCommand(event.getMessage().getContentStripped()))
            return;

        DiscordPlayer sender = plugin.getDiscordPlayerManager().getDiscordPlayer(event.getAuthor().getIdLong());

        if (sender == null) return;

        User essSender = plugin.getEssentials().getUser(sender);
        if (essSender == null) return;

        if (essSender.getReplyRecipient() == null || !essSender.getReplyRecipient().isReachable()) {
            MessageEmbed embed = JdaUtils.getReplyEmbedBuilder()
                    .setDescription("Unable to find anyone to reply to.\n\n" +
                            "If you want to start a conversation, please use `-msg <name>` in here.")
                    .setFooter("Requested by: You", event.getAuthor().getEffectiveAvatarUrl()).build();

            event.getChannel().sendMessage(embed).queue();
            return;
        }

        sender.replyToPrivateMessage(event.getChannel(), event.getMessage().getContentRaw(), essSender);
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
