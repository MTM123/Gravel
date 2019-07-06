package lv.mtm123.cvcancer.listeners;

import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.players.DiscordPlayerManager;
import net.ess3.api.events.AfkStatusChangeEvent;
import net.ess3.api.events.NickChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerListener implements Listener {

    private final CVCancer plugin;

    public PlayerListener(CVCancer plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().setPlayerListName(plugin.getPlayerDiscordDisplayName(event.getPlayer()));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            DiscordPlayerManager manager = plugin.getDiscordPlayerManager();
            manager.getVisiblePlayers().forEach(p -> manager.showDiscordPlayer(p, event.getPlayer()));
        }, 20L);
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
