package lv.mtm123.cvcancer.listeners;

import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.config.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.awt.*;
import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

public class ServerStatusListener implements Listener {

    private final CVCancer plugin;
    private final JDA jda;
    private final TextChannel statusChannel;
    private final long statusMessage;

    public ServerStatusListener(CVCancer plugin, JDA jda, Config config) {
        this.plugin = plugin;
        this.jda = jda;

        statusChannel = jda.getTextChannelById(config.getStatusChannel());
        if (statusChannel == null) {
            throw new AssertionError("An invalid status channel ID was introduced");
        }

        statusMessage = statusChannel.hasLatestMessage()
                ? statusChannel.getLatestMessageIdLong()
                : statusChannel.sendMessage("Temporary message, please stand by").complete().getIdLong();

        //Update stats when the bot starts
        update();
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        update();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::update);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onAfkStatusChange(AfkStatusChangeEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::update);
    }

    private void update() {
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();
        boolean isOnePlayer = players.size() == 1;

        jda.getPresence().setActivity(Activity.playing(players.isEmpty()
                ? "alone" : "with " + players.size() + " other" + (!isOnePlayer ? "s" : "")));

        EmbedBuilder embed = new EmbedBuilder()
                .setAuthor("Server Status")
                .setColor(Color.BLACK)
                .setFooter("Last updated:")
                .setTimestamp(Instant.now())
                .addField("Online players", players.stream()
                        .map(p -> {
                            String name = plugin.getPlayerDiscordDisplayName(p);
                            if (plugin.getEssentials().getUser(p).isAfk()) {
                                name += " (AFK)";
                            }
                            return name;
                        })
                        .collect(Collectors.joining(", ")), false);

        Message m = new MessageBuilder().setContent("\u200B").setEmbed(embed.build()).build();
        statusChannel.editMessageById(statusMessage, m).queue();
    }

}
