package lv.mtm123.cvcancer.cmd;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import lv.mtm123.cvcancer.CVCancer;
import lv.mtm123.cvcancer.config.Config;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.time.Instant;

public class MentionsCommand extends BaseCommand {

    private Config config;
    private CVCancer plugin;

    public MentionsCommand(CVCancer plugin) {
        this.plugin = plugin;
        config = plugin.getPluginConfig();
    }

    @CommandAlias("mention|mentions")
    public void execute(MessageReceivedEvent event) {
        long userId = event.getAuthor().getIdLong();
        boolean isExcluded = config.getChatLinkMentionExclusions().contains(userId);

        replyWithEmbed(event, String.format(
                "You have MC notifications %s.\n" +
                        "In order to %s them, please run %s %s",
                !isExcluded ? "enabled" : "disabled",
                isExcluded ? "enable" : "disable",
                event.getMessage().getContentStripped().trim(),
                isExcluded ? "on" : "off"
        ));
    }

    @CommandAlias("mention|mentions")
    public void execute(MessageReceivedEvent event, boolean toEnable) {
        long userId = event.getAuthor().getIdLong();
        boolean isExcluded = config.getChatLinkMentionExclusions().contains(userId);

        if (toEnable == !isExcluded) {
            replyWithEmbed(event, String.format("You already have MC notifications turned %s!",
                    isExcluded ? "off" : "on"));
            return;
        }

        if (toEnable)
            config.getChatLinkMentionExclusions().removeIf(aLong -> aLong.equals(userId));
        else
            config.getChatLinkMentionExclusions().add(userId);

        plugin.savePluginConfig();

        replyWithEmbed(event, String.format("You have %s MC notifications!",
                toEnable ? "enabled" : "disabled"));
    }

    private void replyWithEmbed(MessageReceivedEvent event, String msg) {
        String effectiveName = event.getMember() != null ? event.getMember().getEffectiveName() :
                event.getAuthor().getName();
        MessageEmbed embed = new EmbedBuilder()
                .setColor(new Color(240, 71, 71)) //DND color
                .setTimestamp(Instant.now())
                .setDescription(msg)
                .setFooter("Requested by: " + effectiveName, event.getAuthor().getEffectiveAvatarUrl()).build();
        event.getMessage().getChannel().sendMessage(embed).queue();
    }

}
