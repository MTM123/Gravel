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

        if (event.getChannel().getIdLong() != 585816708954980360L
                || event.getMessage().getAuthor().isBot()) {
            return;
        }

        String name = event.getMember() == null
                ? event.getAuthor().getName()
                : event.getMember().getEffectiveName();
        String message = ChatColor.translateAlternateColorCodes('&', event.getMessage().getContentStripped());

        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.broadcastMessage(String.format("<%s> %s",
                ChatColor.GRAY + "@" + ChatColor.RESET + name, message)));
    }

}
