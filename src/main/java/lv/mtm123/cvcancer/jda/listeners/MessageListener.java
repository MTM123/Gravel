package lv.mtm123.cvcancer.jda.listeners;

import lv.mtm123.cvcancer.CVCancer;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import javax.annotation.Nonnull;

public class MessageListener extends ListenerAdapter {

    private final CVCancer plugin;

    public MessageListener(CVCancer plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {

        if (event.getMessage().getAuthor().isBot()) {
            return;
        }

        if (event.getChannel().getIdLong() != 585816708954980360L) {
            return;
        }

        String name;

        if (event.getMember() == null) {
            name = event.getAuthor().getName();
        } else {
            name = event.getMember().getEffectiveName();
        }


        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(ChatColor.translateAlternateColorCodes('&',
                String.format("&6[&7DC&6]&r<%s> %s",
                        name,
                        event.getMessage().getContentStripped()))));


    }

}
