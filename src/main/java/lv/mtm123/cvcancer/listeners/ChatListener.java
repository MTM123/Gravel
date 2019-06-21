package lv.mtm123.cvcancer.listeners;

import com.google.gson.JsonObject;
import lv.mtm123.cvcancer.CVCancer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ChatListener implements Listener {

    private final CVCancer plugin;
    private URL webhookUrl;

    public ChatListener(CVCancer plugin, String webhookUrl) {
        this.plugin = plugin;

        try {
            this.webhookUrl = new URL(webhookUrl);
        } catch (MalformedURLException e) {
            this.webhookUrl = null;
            plugin.getLogger().log(Level.SEVERE, "Incorrect webhook URL: {0}", webhookUrl);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {

        if (webhookUrl == null) {
            return;
        }

        JsonObject json = new JsonObject();
        json.addProperty("content", ChatColor.stripColor(event.getMessage()));
        json.addProperty("username", ChatColor.stripColor(plugin.getPlayerDiscordDisplayName(event.getPlayer())));

        byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

            try {

                HttpURLConnection conn = (HttpURLConnection) webhookUrl.openConnection();
                conn.setDoOutput(true);
                conn.setRequestMethod("POST");
                conn.addRequestProperty("Content-Type", "application/json");
                conn.addRequestProperty("User-Agent", "Duck hook");

                try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
                    out.write(data);
                    out.flush();
                }

                conn.getInputStream().close();
                conn.disconnect();

            } catch (IOException e) {
                e.printStackTrace();
            }

        });

    }

}
