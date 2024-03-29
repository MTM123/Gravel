package lv.mtm123.gravel.jda.listeners;

import lv.mtm123.gravel.Gravel;
import lv.mtm123.gravel.config.Config;
import lv.mtm123.gravel.jda.MarkdownConverter;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class MessageListener extends ListenerAdapter {

    private final Gravel plugin;
    private final Config config;
    private final MarkdownConverter converter;

    public MessageListener(Gravel plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.converter = new MarkdownConverter();
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
        final String[] message = {ChatColor.translateAlternateColorCodes('&',
                converter.compute(event.getMessage().getContentDisplay()))};

        ArrayList<Object> attachmentUrls = event.getMessage().getAttachments().stream()
                .map(this::getEffectiveUrl).collect(Collectors.toCollection(ArrayList::new));

        //Add attachments to the end of the message
        attachmentUrls.forEach(e -> message[0] += " " + e);

        Bukkit.getScheduler().runTask(plugin,
                () -> {
                    BaseComponent[] msg = new ComponentBuilder("<" +
                            ChatColor.GRAY + "@" + ChatColor.RESET + name + ">")
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new BaseComponent[]{new TextComponent(
                                            ChatColor.GRAY + "Click here to mention this user")}))
                            .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "@" + name + " "))
                            .append(" ")
                            .append(TextComponent.fromLegacyText(message[0].trim()), ComponentBuilder.FormatRetention.NONE)
                            .create();
                    Bukkit.broadcast(msg);
                });
    }

    private String getEffectiveUrl(Message.Attachment a) {
        return a.isImage() ? a.getProxyUrl() : a.getUrl();
    }

}
