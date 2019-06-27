package lv.mtm123.cvcancer.players;

import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.players.packets.WrapperPlayServerPlayerInfo;
import net.dv8tion.jda.api.entities.Member;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

public class DiscordPlayer extends CustomPlayer {
    private Member discordMember;
    private UUID discordUuid;

    DiscordPlayer(Member discordMember) {
        this.discordMember = discordMember;
        discordUuid = UUID.nameUUIDFromBytes(discordMember.getId().getBytes(StandardCharsets.UTF_8));
    }

    public Member getDiscordMember() {
        return discordMember;
    }

    @Override
    public void sendRawMessage(String message) {
        try {
            discordMember.getUser().openPrivateChannel().complete().sendMessage(ChatColor.stripColor(message)).queue();
        } catch (Exception ignored) {

        }
    }

    @Override
    public boolean isOnline() {
        return !CVCancer.getPluginInstance().getPluginConfig().getChatLinkMentionExclusions().contains(discordMember.getIdLong());
    }

    @Override
    public String getName() {
        return "@" + discordMember.getEffectiveName();
    }

    void createFakeEntity(Player... players) {
        sendPlayerInfoPacket(EnumWrappers.PlayerInfoAction.ADD_PLAYER, players);
    }

    void destroyFakeEntity(Player... players) {
        sendPlayerInfoPacket(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, players);
    }

    private void sendPlayerInfoPacket(EnumWrappers.PlayerInfoAction action, Player[] targets) {
        ProtocolManager manager = CVCancer.getPluginInstance().getProtocolManager();

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
                    WrapperPlayServerPlayerInfo playerDisplay = new WrapperPlayServerPlayerInfo();
                    playerDisplay.setAction(EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME);
                    playerDisplay.setData(Collections.singletonList(new PlayerInfoData(
                            new WrappedGameProfile(getUniqueId(), getName()),
                            0,
                            EnumWrappers.NativeGameMode.SURVIVAL,
                            WrappedChatComponent.fromText(ChatColor.GRAY + "@" + ChatColor.RESET + getDiscordMember().getEffectiveName()/* + " on Discord"*/)
                    )));
                    manager.sendServerPacket(p, playerDisplay.getHandle());
                }

            } catch (InvocationTargetException e) {
                CVCancer.getPluginInstance().getLogger().severe("Unable to send packets to player: " + e.getMessage());
            }
        }
    }

    @Override
    public UUID getUniqueId() {
        return discordUuid;
    }
}
