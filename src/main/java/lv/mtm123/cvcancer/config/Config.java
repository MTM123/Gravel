package lv.mtm123.cvcancer.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

import java.util.ArrayList;
import java.util.List;

@ConfigSerializable
public class Config {

    @Setting("bot-token")
    private String botToken = "REPLACE WITH BOT TOKEN";

    @Setting("webhook-url")
    private String webhookUrl = "REPLACE WITH WEBHOOKURL";

    @Setting("channels.status-id")
    private long statusChannel = 592147576665538599L;

    @Setting("channels.link-id")
    private long chatLinkChannel = 592166955432017920L;
    @Setting("chat-link-exclusions")
    private List<Long> chatLinkMentionExclusions = new ArrayList<>();

    public List<Long> getChatLinkMentionExclusions() {
        return chatLinkMentionExclusions;
    }

    public long getStatusChannel() {
        return statusChannel;
    }

    public long getChatLinkChannel() {
        return chatLinkChannel;
    }

    public String getBotToken() {
        return botToken;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public boolean canReceiveMentions(long id) {
        return !getChatLinkMentionExclusions().contains(id);
    }

}
