package lv.mtm123.cvcancer.jda.cmd;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Single;
import com.earth2me.essentials.User;
import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.jda.JdaUtils;
import lv.mtm123.cvcancer.players.DiscordPlayer;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MessageCommand extends BaseCommand {
    private CVCancer plugin;

    public MessageCommand(CVCancer cvCancer) {
        plugin = cvCancer;
    }


    @CommandAlias("msg|message")
    public void execute(MessageReceivedEvent event, @Single String player, String otherText) {
        execute(event, player);

        DiscordPlayer sender = plugin.getDiscordPlayerManager().getDiscordPlayer(event.getAuthor().getIdLong());
        User essSender = plugin.getEssentials().getUser(sender);
        if (sender != null && essSender != null) {
            sender.replyToPrivateMessage(event.getChannel(), otherText, essSender);
        }
    }

    @CommandAlias("msg|message")
    public void execute(MessageReceivedEvent event, String player) {
        //This command can only be used on private messages
        if (!event.isFromType(ChannelType.PRIVATE)) {
            replyWithEmbed(event, "This command can only be used on private messages with me.");
            return;
        }

        User target = plugin.getEssentials().getUser(player);

        DiscordPlayer sender = plugin.getDiscordPlayerManager().getDiscordPlayer(event.getAuthor().getIdLong());

        if (sender == null) return;

        if (target == null || target.getBase() == null) {
            replyWithEmbed(event, "Unable to find that player.");
            return;
        }

        plugin.getEssentials().getUser(sender).setReplyRecipient(target);

        String name = plugin.getPlayerDiscordDisplayName(target.getBase());
        replyWithEmbed(event, "You have started a private message with " + name + ".\n" +
                "Now you two can have a conversation.\n" +
                "When you send a message in here, it'll be forwarded to the player on the server.");

    }

    private void replyWithEmbed(MessageReceivedEvent event, String msg) {
        String effectiveName = event.isFromType(ChannelType.PRIVATE) ? "You" : event.getMember() != null ?
                event.getMember().getEffectiveName() :
                event.getAuthor().getName();
        MessageEmbed embed = JdaUtils.getReplyEmbedBuilder()
                .setDescription(msg)
                .setFooter("Requested by: " + effectiveName, event.getAuthor().getEffectiveAvatarUrl()).build();
        event.getMessage().getChannel().sendMessage(embed).queue();
    }

}
