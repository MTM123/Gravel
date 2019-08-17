package lv.mtm123.gravel.listeners;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import club.minnced.discord.webhook.send.WebhookMessage;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import lv.mtm123.gravel.Gravel;
import lv.mtm123.gravel.config.Config;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

public class ChatListener implements Listener {

    private final Gravel plugin;
    private final JDA jda;
    private final Config config;
    private final List<Emote> emotes;
    private WebhookClient client;

    public ChatListener(Gravel plugin, JDA jda, Config config) {
        this.plugin = plugin;
        this.jda = jda;
        emotes = Objects.requireNonNull(jda.getTextChannelById(config.getChatLinkChannel())).getGuild().getEmotes();

        this.config = config;

        try {
            client = new WebhookClientBuilder(config.getWebhookUrl()).build();
        } catch (IllegalArgumentException e) {
            plugin.getLogger().log(Level.SEVERE, "Incorrect webhook URL: {0}", config.getWebhookUrl());
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChat(AsyncPlayerChatEvent event) {
        String msg = event.getMessage()
                .replace("@everyone", "at-everyone")
                .replace("@here", "at-here");

        msg = processDiscordTags(msg);
        msg = processDiscordEmotes(msg);

        @NotNull WebhookMessage message = new WebhookMessageBuilder()
                .setUsername(ChatColor.stripColor(plugin.getPlayerDiscordDisplayName(event.getPlayer())))
                .setAvatarUrl("https://www.mc-heads.net/head/" + event.getPlayer().getName())
                .setContent(ChatColor.stripColor(msg)).build();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> client.send(message));

    }

    private String processDiscordTags(String msg) {
        final String[] newMsg = {msg};
        TextChannel channel = jda.getTextChannelById(config.getChatLinkChannel());
        if (channel != null) {
            channel.getMembers().forEach(m -> newMsg[0] = newMsg[0].replace("@" + m.getEffectiveName(),
                    m.getAsMention()));
        }
        return newMsg[0];
    }

    private String processDiscordEmotes(String message) {
        final String[] newMessage = {message};
        emotes.forEach(e -> newMessage[0] = newMessage[0].replace(":" + e.getName() + ":", e.getAsMention()));
        return newMessage[0];
    }
}
