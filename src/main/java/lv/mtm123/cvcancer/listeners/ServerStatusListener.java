package lv.mtm123.cvcancer.listeners;

import lv.mtm123.cvcancer.CVCancer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.ess3.api.events.AfkStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.awt.*;
import java.time.Instant;
import java.util.Collection;
import java.util.stream.Collectors;

public class ServerStatusListener implements Listener {
	private final CVCancer plugin;
	private final JDA jda;
	private final TextChannel statusChannel;
	private final long statusMessage;
	
	public ServerStatusListener(CVCancer plugin, JDA jda) {
		this.plugin = plugin;
		this.jda = jda;
		
		statusChannel = jda.getTextChannelById("A TOTALLY NOT HARDCODED ID");
		if (statusChannel == null) {
			throw new AssertionError("An invalid status channel ID was hardcoded");
		}
		
		statusMessage = statusChannel.hasLatestMessage()
				? statusChannel.getLatestMessageIdLong()
				: statusChannel.sendMessage("Temporary message, please stand by").complete().getIdLong();
	}
	
	
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		update();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerJoinEvent event) {
		update();
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onAfkStatusChange(AfkStatusChangeEvent event) {
		update();
	}
	
	
	
	private void update() {
		Collection<? extends Player> players = Bukkit.getOnlinePlayers();
		
		jda.getPresence().setActivity(Activity.playing(players.isEmpty()
				? "alone" : "with " + players.size() + " others"));
		
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
		
		statusChannel.editMessageById(statusMessage, embed.build()).queue();
	}
}
