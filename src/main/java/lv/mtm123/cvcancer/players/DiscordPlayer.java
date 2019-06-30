package lv.mtm123.cvcancer.players;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.messaging.IMessageRecipient;
import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.config.Config;
import lv.mtm123.cvcancer.jda.JdaUtils;
import lv.mtm123.cvcancer.players.packets.WrapperPlayServerPlayerInfo;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
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

import static com.earth2me.essentials.I18n.tl;
import static lv.mtm123.cvcancer.CVCancer.getPluginInstance;

public class DiscordPlayer extends CustomPlayer {
    private UUID discordUuid;
    private long memberId;
    private JDA jda;
    private TextChannel chatLinkChannel;
    private Config pluginConfig;
    private CVCancer plugin;

    DiscordPlayer(long memberId) {
        plugin = getPluginInstance();
        jda = plugin.getJda();
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

    public void replyToPrivateMessage(MessageChannel channel, String msg, com.earth2me.essentials.User essSender) {
        IMessageRecipient.MessageResponse messageResponse = essSender.getReplyRecipient().onReceiveMessage(essSender,
                msg);

        switch (messageResponse) {
            case MESSAGES_IGNORED:
            case SENDER_IGNORED:
            case UNREACHABLE:
                MessageEmbed embed = JdaUtils.getReplyEmbedBuilder()
                        .setDescription("Unable to reply to this person.\n\n" +
                                "If you want to start a conversation, please use `-msg <name>` in here.")
                        .setFooter("Requested by: You", getDiscordUser().getEffectiveAvatarUrl()).build();

                channel.sendMessage(embed).queue();
                break;
            default:
                com.earth2me.essentials.User recipientUser =
                        plugin.getEssentials().getUser(essSender.getReplyRecipient().getName());
                // Dont spy on chats involving socialspy exempt players
                boolean shouldSocialSpy =
                        !essSender.isAuthorized("essentials.chat.spy.exempt") &&
                                recipientUser != null && !recipientUser.isAuthorized("essentials.chat.spy.exempt");
                if (shouldSocialSpy) {
                    Essentials ess = plugin.getEssentials();
                    for (com.earth2me.essentials.User onlineUser : ess.getOnlineUsers()) {
                        if (onlineUser.isSocialSpyEnabled()
                                // Don't send socialspy messages to message sender/receiver to prevent spam
                                && !onlineUser.equals(essSender)
                                && !onlineUser.equals(recipientUser)) {
                            if (essSender.isMuted() && ess.getSettings().getSocialSpyListenMutedPlayers()) {
                                onlineUser.sendMessage(tl("socialMutedSpyPrefix") + tl("socialSpyMsgFormat",
                                        getName(), recipientUser.getDisplayName(), msg));
                            } else {
                                onlineUser.sendMessage(tl("socialSpyPrefix") + tl("socialSpyMsgFormat",
                                        getName(), recipientUser.getDisplayName(), msg));
                            }
                        }
                    }
                }
                break;
        }
    }

}
