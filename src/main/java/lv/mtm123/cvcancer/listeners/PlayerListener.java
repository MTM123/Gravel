package lv.mtm123.cvcancer.listeners;

import lv.mtm123.cvcancer.CVCancer;
import net.ess3.api.events.NickChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerListener implements Listener {
    private final CVCancer plugin;

    public PlayerListener(CVCancer plugin) {
        this.plugin = plugin;
    }

    

    @EventHandler(ignoreCancelled = true)
    public void onNickChange(NickChangeEvent event) {
        event.getAffected().getBase().setPlayerListName(event.getValue());
        //TODO: implement "persistent" nicknames
    }
}
