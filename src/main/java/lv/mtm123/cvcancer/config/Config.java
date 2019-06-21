package lv.mtm123.cvcancer.config;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class Config {

    @Setting("bot-token")
    private String botToken = "REPLACE WITH BOT TOKEN";

    @Setting("webhook-url")
    private String webhookUrl = "REPLACE WITH WEBHOOKURL";

    public String getBotToken() {
        return botToken;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

}
