package lv.mtm123.cvcancer.listeners;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.util.WebhookMessage;
import net.ess3.api.events.NickChangeEvent;
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

public class PlayerListener implements Listener {

    private static final Gson GSON = new GsonBuilder().create();

    private final CVCancer plugin;
    private URL webHookUrl;

    public PlayerListener(CVCancer plugin, String webhookUrl) {
        this.plugin = plugin;
        try {
            this.webHookUrl = new URL(webhookUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            this.webHookUrl = null;
            plugin.getLogger().log(Level.SEVERE, String.format("Incorrect webhook URL: '%s'", webhookUrl));
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {

        String username = plugin.getEssentials().getUser(event.getPlayer()).getNickname();
        if (username == null) {
            username = event.getPlayer().getName();
        }

        username = ChatColor.stripColor(username);
        String msg = ChatColor.stripColor(event.getMessage());

        WebhookMessage message = new WebhookMessage(msg, username);
        byte[] data = GSON.toJson(message).getBytes(StandardCharsets.UTF_8);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {

                HttpURLConnection conn = (HttpURLConnection) webHookUrl.openConnection();
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

    @EventHandler(ignoreCancelled = true)
    public void onNickChange(NickChangeEvent event) {
        event.getAffected().getBase().setPlayerListName(event.getValue());
        //TODO: implement "persistent" nicknames
    }

}
