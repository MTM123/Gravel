package lv.mtm123.cvcancer.listeners;

import com.google.gson.JsonObject;
import lv.mtm123.cvcancer.CVCancer;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ChatListener implements Listener {
	private final CVCancer plugin;
	private final HttpRequest.Builder baseRequest;
	private final HttpClient client = HttpClient.newHttpClient();
	
	public ChatListener(CVCancer plugin, String webhookUrl) {
		this.plugin = plugin;
		
		baseRequest = HttpRequest.newBuilder()
				.uri(URI.create(webhookUrl))
				.header("Content-Type", "application/json")
				.header("User-Agent", "Duck hook");
	}
	
	
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onChat(AsyncPlayerChatEvent event) {
		JsonObject json = new JsonObject();
		json.addProperty("content", ChatColor.stripColor(event.getMessage()));
		json.addProperty("username", ChatColor.stripColor(plugin.getPlayerDiscordDisplayName(event.getPlayer())));
		byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);
		
		HttpRequest request = baseRequest.POST(HttpRequest.BodyPublishers.ofByteArray(data)).build();
		client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
				.whenComplete((response, throwable) -> {
					if (throwable != null) {
						plugin.getLogger().log(Level.WARNING,
								"Error while forwarding chat message to Discord", throwable);
					}
				});
	}
}
