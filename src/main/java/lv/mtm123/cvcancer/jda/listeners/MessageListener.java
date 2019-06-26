package lv.mtm123.cvcancer.jda.listeners;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.messaging.IMessageRecipient;
import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.config.Config;
import lv.mtm123.cvcancer.jda.JdaUtils;
import lv.mtm123.cvcancer.jda.MarkdownConverter;
import lv.mtm123.cvcancer.players.DiscordPlayer;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import javax.annotation.Nonnull;

import static com.earth2me.essentials.I18n.tl;

public class MessageListener extends ListenerAdapter {

    private final CVCancer plugin;
    private final Config config;
    private final MarkdownConverter converter;

    public MessageListener(CVCancer plugin, Config config) {
        this.plugin = plugin;
        this.config = config;
        this.converter = new MarkdownConverter();
    }

    @Override
    public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event) {
        if (event.getMessage().getAuthor().isBot()) return;

        DiscordPlayer sender = plugin.getDiscordPlayerManager().getDiscordPlayer(event.getAuthor().getIdLong());

        if (sender == null) return;

        User essSender = plugin.getEssentials().getUser(sender);
        if (essSender == null) return;

        if (essSender.getReplyRecipient() == null || !essSender.getReplyRecipient().isReachable()) {
            MessageEmbed embed = JdaUtils.getReplyEmbedBuilder()
                    .setDescription("Unable to find anyone to reply to.\n\n" +
                            "If you want to start a conversation, please use `-msg <name>` in here.")
                    .setFooter("Requested by: You", event.getAuthor().getEffectiveAvatarUrl()).build();

            event.getChannel().sendMessage(embed).queue();
            return;
        }

        //We either got a message or a command. Ignore message commands
        if (event.getMessage().getContentStripped().startsWith("-message") || event.getMessage().getContentStripped().startsWith("-msg"))
            return;


        String message = event.getMessage().getContentRaw();
        IMessageRecipient.MessageResponse messageResponse = essSender.getReplyRecipient().onReceiveMessage(essSender, message);

        switch (messageResponse) {
            case MESSAGES_IGNORED:
            case SENDER_IGNORED:
            case UNREACHABLE:
                MessageEmbed embed = JdaUtils.getReplyEmbedBuilder()
                        .setDescription("Unable to reply to this person.\n\n" +
                                "If you want to start a conversation, please use `-msg <name>` in here.")
                        .setFooter("Requested by: You", event.getAuthor().getEffectiveAvatarUrl()).build();

                event.getChannel().sendMessage(embed).queue();
                break;
            default:
                User recipientUser = plugin.getEssentials().getUser(essSender.getReplyRecipient().getName());
                // Dont spy on chats involving socialspy exempt players
                if (!essSender.isAuthorized("essentials.chat.spy.exempt") && recipientUser != null && !recipientUser.isAuthorized("essentials.chat.spy.exempt")) {
                    Essentials ess = plugin.getEssentials();
                    for (User onlineUser : ess.getOnlineUsers()) {
                        if (onlineUser.isSocialSpyEnabled()
                                // Don't send socialspy messages to message sender/receiver to prevent spam
                                && !onlineUser.equals(essSender)
                                && !onlineUser.equals(recipientUser)) {
                            if (essSender.isMuted() && ess.getSettings().getSocialSpyListenMutedPlayers()) {
                                onlineUser.sendMessage(tl("socialMutedSpyPrefix") + tl("socialSpyMsgFormat", sender.getName(), recipientUser.getDisplayName(), message));
                            } else {
                                onlineUser.sendMessage(tl("socialSpyPrefix") + tl("socialSpyMsgFormat", sender.getName(), recipientUser.getDisplayName(), message));
                            }
                        }
                    }
                }
                break;
        }
    }

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        if (event.getChannel().getIdLong() != config.getChatLinkChannel()
                || event.getMessage().getAuthor().isBot()) {
            return;
        }

        String name = event.getMember() == null
                ? event.getAuthor().getName()
                : event.getMember().getEffectiveName();
        String message = ChatColor.translateAlternateColorCodes('&',
                converter.compute(event.getMessage().getContentDisplay()));

        Bukkit.getScheduler().runTask(plugin,
                () -> {
                    BaseComponent[] msg = new ComponentBuilder("<" +
                            ChatColor.GRAY + "@" + ChatColor.RESET + name + ">")
                            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                    new BaseComponent[]{new TextComponent(
                                            ChatColor.GRAY + "Click here to mention this user")}))
                            .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "@" + name + " "))
                            .append(" ")
                            .append(TextComponent.fromLegacyText(message), ComponentBuilder.FormatRetention.NONE)
                            .create();
                    Bukkit.broadcast(msg);
                });
    }

}
