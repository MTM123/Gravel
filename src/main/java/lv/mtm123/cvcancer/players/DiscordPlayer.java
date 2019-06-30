package lv.mtm123.cvcancer.players;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import lv.mtm123.cvcancer.config.Config;
import lv.mtm123.cvcancer.players.packets.WrapperPlayServerPlayerInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

import static lv.mtm123.cvcancer.CVCancer.getPluginInstance;

public class DiscordPlayer extends CustomPlayer {
    private UUID discordUuid;
    private long memberId;
    private JDA jda;
    private TextChannel chatLinkChannel;
    private Config pluginConfig;

    DiscordPlayer(long memberId) {
        jda = getPluginInstance().getJda();
        assert jda != null;
        this.pluginConfig = getPluginInstance().getPluginConfig();
        this.chatLinkChannel = jda.getTextChannelById(pluginConfig.getChatLinkChannel());
        this.memberId = memberId;
        this.discordUuid = UUID.nameUUIDFromBytes(String.valueOf(memberId).getBytes(StandardCharsets.UTF_8));
        setupTabListUpdateEvent();
    }

    private void setupTabListUpdateEvent() {
        jda.addEventListener(new ListenerAdapter() {
            @Override
            public void onUserUpdateName(@Nonnull UserUpdateNameEvent event) {
                updateDisplayName(getDiscordDisplayName(), Bukkit.getOnlinePlayers().toArray(new Player[]{}));
            }
        });
    }

    private Member getDiscordMember() {
        return chatLinkChannel.getGuild().getMember(getDiscordUser());
    }

    @NotNull
    private User getDiscordUser() {
        return Objects.requireNonNull(jda.getUserById(memberId));
    }

    @Override
    public void sendRawMessage(String message) {
        try {
            getDiscordUser().openPrivateChannel().complete().sendMessage(ChatColor.stripColor(message)).queue();
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean isOnline() {
        return pluginConfig.canReceiveMentions(memberId);
    }

    @Override
    public String getName() {
        return "@" + getDiscordMember().getEffectiveName();
    }

    void createFakeEntity(Player... players) {
        sendPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, players);
    }

    void destroyFakeEntity(Player... players) {
        sendPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, players);
    }

    private void sendPlayerInfoPacket(EnumWrappers.PlayerInfoAction action, Player[] targets) {
        ProtocolManager manager = getPluginInstance().getProtocolManager();

        WrapperPlayServerPlayerInfo playerInfo = new WrapperPlayServerPlayerInfo();
        playerInfo.setAction(action);
        playerInfo.setData(Collections.singletonList(new PlayerInfoData(
                new WrappedGameProfile(getUniqueId(), getName()),
                0,
                EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText(getName())
        )));

        for (Player p : targets) {
            try {
                manager.sendServerPacket(p, playerInfo.getHandle());

                if (action == EnumWrappers.PlayerInfoAction.ADD_PLAYER) {
                    updateDisplayName(getDiscordDisplayName(), p);
                }

            } catch (InvocationTargetException e) {
                getPluginInstance().getLogger().severe("Unable to send packets to player: " + e.getMessage());
            }
        }
    }

    @Override
    public UUID getUniqueId() {
        return discordUuid;
    }

    @NotNull
    private String getDiscordDisplayName() {
        return ChatColor.GRAY + "@" + ChatColor.RESET + getDiscordMember().getEffectiveName();
    }

    private void updateDisplayName(String name, Player... players) {
        ProtocolManager manager = getPluginInstance().getProtocolManager();

        WrapperPlayServerPlayerInfo playerDisplay = new WrapperPlayServerPlayerInfo();
        playerDisplay.setAction(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);
        playerDisplay.setData(Collections.singletonList(new PlayerInfoData(
                new WrappedGameProfile(getUniqueId(), getName()),
                0,
                EnumWrappers.NativeGameMode.SURVIVAL,
                WrappedChatComponent.fromText(name)
        )));

        for (Player p : players) {
            try {
                manager.sendServerPacket(p, playerDisplay.getHandle());
            } catch (InvocationTargetException e) {
                getPluginInstance().getLogger().severe(String.format("Unable to send packet to %s.",
                        p.getName()));
            }
        }
    }
}
