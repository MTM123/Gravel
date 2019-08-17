package lv.mtm123.gravel.listeners;

import lv.mtm123.gravel.Gravel;
import net.ess3.api.events.AfkStatusChangeEvent;
import net.ess3.api.events.NickChangeEvent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final Gravel plugin;

    public PlayerListener(Gravel plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().setPlayerListName(plugin.getPlayerDiscordDisplayName(event.getPlayer()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onAfkStatusChange(AfkStatusChangeEvent event) {
        Player player = event.getAffected().getBase();
        String suffix = event.getValue() ? ChatColor.GRAY + " [AFK]" : "";
        player.setPlayerListName(plugin.getPlayerDiscordDisplayName(player) + suffix);
    }

    @EventHandler(ignoreCancelled = true)
    public void onNickChange(NickChangeEvent event) {
        event.getAffected().getBase().setPlayerListName(event.getValue());
        //TODO: implement "persistent" nicknames
    }

}
