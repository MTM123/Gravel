package lv.mtm123.cvcancer.util;

public class WebhookMessage {

    private final String content;
    private final String username;

    public WebhookMessage(String content, String username) {
        this.content = content;
        this.username = username;
    }

}
