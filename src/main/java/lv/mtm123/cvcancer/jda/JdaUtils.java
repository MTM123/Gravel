package lv.mtm123.cvcancer.jda;

import net.dv8tion.jda.api.EmbedBuilder;

import java.awt.*;
import java.time.Instant;

public class JdaUtils {
    public static EmbedBuilder getReplyEmbedBuilder() {
        return new EmbedBuilder()
                .setColor(new Color(240, 71, 71)) //DND color
                .setTimestamp(Instant.now());
    }
}
