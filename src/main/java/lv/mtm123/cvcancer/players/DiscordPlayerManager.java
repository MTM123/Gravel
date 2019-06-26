package lv.mtm123.cvcancer.players;

import com.earth2me.essentials.User;
import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.config.Config;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DiscordPlayerManager extends ListenerAdapter {

    private CVCancer plugin;
    private Config config;
    private List<DiscordPlayer> visiblePlayers = new ArrayList<>();
    private Map<Long, DiscordPlayer> cachedPlayers = new HashMap<>();

    public DiscordPlayerManager(CVCancer plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        reconstructDiscordPlayers();
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        super.onGuildMemberJoin(event);
        //Update our custom users
        reconstructDiscordPlayers();
    }

    @Override
    public void onGuildMemberLeave(@Nonnull GuildMemberLeaveEvent event) {
        super.onGuildMemberLeave(event);

        DiscordPlayer player = getDiscordPlayer(event.getMember());
        hideDiscordPlayer(player);
        cachedPlayers.remove(event.getMember().getIdLong());
        //Update our custom users
        reconstructDiscordPlayers();
    }

    private void reconstructDiscordPlayers() {
        assert plugin.getJda() != null;
        TextChannel link = plugin.getJda().getTextChannelById(config.getChatLinkChannel());

        if (link != null) {
            ArrayList<DiscordPlayer> players =
                    link.getMembers().stream()
                            .filter(m -> !m.getUser().isBot())
                            .filter(m -> !config.getChatLinkMentionExclusions().contains(m.getIdLong()))
                            .map(this::getDiscordPlayer)
                            .collect(Collectors.toCollection(ArrayList::new));

            plugin.getEssentials().getCustomPlayers().clear();
            for (DiscordPlayer player : players) {
                prepareEssentialsPlayer(player);

                if (!visiblePlayers.contains(player)) {
                    showDiscordPlayer(player);
                }
            }
        }
    }

    private void prepareEssentialsPlayer(DiscordPlayer player) {
        plugin.getEssentials().addCustomPlayer(player);
        User user = plugin.getEssentials().getUser(player);
        user.setNPC(true);
        user.setTeleportEnabled(false);
        user.setAutoTeleportEnabled(false);
        user.setLastMessageReplyRecipient(false);
    }

    public List<DiscordPlayer> getVisiblePlayers() {
        return visiblePlayers;
    }

    public void hideDiscordPlayer(DiscordPlayer player) {
        player.destroyFakeEntity(Bukkit.getOnlinePlayers().toArray(new Player[]{}));
        visiblePlayers.remove(player);
    }

    public void showDiscordPlayer(DiscordPlayer player) {
        player.createFakeEntity(Bukkit.getOnlinePlayers().toArray(new Player[]{}));
        visiblePlayers.add(player);
    }

    public void showDiscordPlayer(DiscordPlayer player, Player target) {
        player.createFakeEntity(target);
    }
    public void hideDiscordPlayer(DiscordPlayer player, Player target) {
        player.destroyFakeEntity(target);
    }

    public DiscordPlayer getDiscordPlayer(Member member) {
        return cachedPlayers.computeIfAbsent(member.getIdLong(), id -> new DiscordPlayer(member));
    }
    @Nullable
    public DiscordPlayer getDiscordPlayer(long userId) {
        return cachedPlayers.getOrDefault(userId, null);
    }
}
