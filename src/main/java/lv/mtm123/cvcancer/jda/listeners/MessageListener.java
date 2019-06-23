package lv.mtm123.cvcancer.jda.listeners;

import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.config.Config;
import lv.mtm123.cvcancer.jda.MarkdownConverter;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import javax.annotation.Nonnull;

public class MessageListener extends ListenerAdapter {

    private final CVCancer plugin;
    private final Config config;
    private final MarkdownConverter converter;

    public MessageListener(CVCancer plugin, Config config) {
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
        String message = ChatColor.translateAlternateColorCodes('&',
                converter.compute(event.getMessage().getContentDisplay()));

        Bukkit.getScheduler().runTask(plugin,
                () -> {
                    BaseComponent[] msg = new ComponentBuilder("<" +
                            ChatColor.GRAY + "@" + name + ChatColor.RESET +
                            ">")
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new BaseComponent[]{new TextComponent(
                                            ChatColor.GRAY + "Click here to mention this user")}))
                            .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "@" + name + " "))
                            .append(" ")
                            .append(message).create();
                    Bukkit.broadcast(msg);
                });
    }

}
