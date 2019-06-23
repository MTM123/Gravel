package lv.mtm123.cvcancer.jda;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.dv8tion.jda.internal.utils.Checks;
import org.bukkit.ChatColor;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MarkdownConverter {

    /**
     * Normal characters that are not special for markdown, ignoring this has no effect
     */
    private static final int NORMAL = 0;
    /**
     * Bold region such as "**Hello**"
     */
    private static final int BOLD = 1;
    /**
     * Italics region for underline such as "_Hello_"
     */
    private static final int ITALICS_U = 1 << 1;
    /**
     * Italics region for asterisks such as "*Hello*"
     */
    private static final int ITALICS_A = 1 << 2;
    /**
     * Monospace region such as "`Hello`"
     */
    private static final int MONO = 1 << 3;
    /**
     * Monospace region such as "``Hello``"
     */
    private static final int MONO_TWO = 1 << 4;
    /**
     * Codeblock region such as "```Hello```"
     */
    private static final int BLOCK = 1 << 5;
    /**
     * Spoiler region such as "||Hello||"
     */
    private static final int SPOILER = 1 << 6;
    /**
     * Underline region such as "__Hello__"
     */
    private static final int UNDERLINE = 1 << 7;
    /**
     * Strikethrough region such as "~~Hello~~"
     */
    private static final int STRIKE = 1 << 8;

    private static final int ESCAPED_BOLD = Integer.MIN_VALUE | BOLD;
    private static final int ESCAPED_ITALICS_U = Integer.MIN_VALUE | ITALICS_U;
    private static final int ESCAPED_ITALICS_A = Integer.MIN_VALUE | ITALICS_A;
    private static final int ESCAPED_MONO = Integer.MIN_VALUE | MONO;
    private static final int ESCAPED_MONO_TWO = Integer.MIN_VALUE | MONO_TWO;
    private static final int ESCAPED_BLOCK = Integer.MIN_VALUE | BLOCK;
    private static final int ESCAPED_SPOILER = Integer.MIN_VALUE | SPOILER;
    private static final int ESCAPED_UNDERLINE = Integer.MIN_VALUE | UNDERLINE;
    private static final int ESCAPED_STRIKE = Integer.MIN_VALUE | STRIKE;

    private static final Pattern codeLanguage = Pattern.compile("^\\w+\n.*", Pattern.MULTILINE | Pattern.DOTALL);

    private static final TIntObjectMap<String> tokens;

    static {
        tokens = new TIntObjectHashMap<>();
        tokens.put(NORMAL, "");
        tokens.put(BOLD, "**");
        tokens.put(ITALICS_U, "_");
        tokens.put(ITALICS_A, "*");
        tokens.put(BOLD | ITALICS_A, "***");
        tokens.put(MONO, "`");
        tokens.put(MONO_TWO, "``");
        tokens.put(BLOCK, "```");
        tokens.put(SPOILER, "||");
        tokens.put(UNDERLINE, "__");
        tokens.put(STRIKE, "~~");
    }

    private int ignored;

    public MarkdownConverter() {
        this.ignored = NORMAL;
    }

    private MarkdownConverter(int ignored) {
        this.ignored = ignored;
    }

    /**
     * Sanitize string without ignoring anything.
     *
     * @param sequence The string to sanitize
     * @return The sanitized string
     * @throws java.lang.IllegalArgumentException If provided with null
     * @see MarkdownConverter#MarkdownConverter()
     * @see #withIgnored(int)
     */
    @Nonnull
    private static String sanitize(@Nonnull String sequence) {
        Checks.notNull(sequence, "String");
        return new MarkdownConverter().compute(sequence);
    }

    /**
     * Escapes every markdown formatting found in the provided string.
     *
     * @param sequence The string to sanitize
     * @return The string with escaped markdown
     * @throws java.lang.IllegalArgumentException If provided with null
     * @see #escape(String, int)
     */
    @Nonnull
    public static String escape(@Nonnull String sequence) {
        return escape(sequence, NORMAL);
    }

    /**
     * Escapes every markdown formatting found in the provided string.
     * <br>Example: {@code escape("**Hello** ~~World~~!", MarkdownConverter.BOLD | MarkdownConverter.STRIKE)}
     *
     * @param sequence The string to sanitize
     * @param ignored  Formats to ignore
     * @return The string with escaped markdown
     * @throws java.lang.IllegalArgumentException If provided with null
     */
    @Nonnull
    private static String escape(@Nonnull String sequence, int ignored) {
        return new MarkdownConverter()
                .withIgnored(ignored)
                .compute(sequence);
    }

    /**
     * Specific regions to ignore.
     * <br>Example: {@code new MarkdownConverter().withIgnored(MarkdownConverter.BOLD | MarkdownConverter.UNDERLINE)
     * .compute("Hello __world__!")}
     *
     * @param ignored The regions to ignore
     * @return The current sanitizer instance with the new ignored regions
     */
    @Nonnull
    private MarkdownConverter withIgnored(int ignored) {
        this.ignored |= ignored;
        return this;
    }

    private int getRegion(int index, @Nonnull String sequence) {
        if (sequence.length() - index >= 3) {
            String threeChars = sequence.substring(index, index + 3);
            switch (threeChars) {
                case "```":
                    return doesEscape(index, sequence) ? ESCAPED_BLOCK : BLOCK;
                case "***":
                    return doesEscape(index, sequence) ? ESCAPED_BOLD | ITALICS_A : BOLD | ITALICS_A;
            }
        }
        if (sequence.length() - index >= 2) {
            String twoChars = sequence.substring(index, index + 2);
            switch (twoChars) {
                case "**":
                    return doesEscape(index, sequence) ? ESCAPED_BOLD : BOLD;
                case "__":
                    return doesEscape(index, sequence) ? ESCAPED_UNDERLINE : UNDERLINE;
                case "~~":
                    return doesEscape(index, sequence) ? ESCAPED_STRIKE : STRIKE;
                case "``":
                    return doesEscape(index, sequence) ? ESCAPED_MONO_TWO : MONO_TWO;
                case "||":
                    return doesEscape(index, sequence) ? ESCAPED_SPOILER : SPOILER;
            }
        }
        char current = sequence.charAt(index);
        switch (current) {
            case '*':
                return doesEscape(index, sequence) ? ESCAPED_ITALICS_A : ITALICS_A;
            case '_':
                return doesEscape(index, sequence) ? ESCAPED_ITALICS_U : ITALICS_U;
            case '`':
                return doesEscape(index, sequence) ? ESCAPED_MONO : MONO;
        }
        return NORMAL;
    }

    private boolean hasCollision(int index, @Nonnull String sequence, char c) {
        if (index < 0)
            return false;
        return index < sequence.length() - 1 && sequence.charAt(index + 1) == c;
    }

    private int findEndIndex(int afterIndex, int region, @Nonnull String sequence) {
        if (isEscape(region))
            return -1;
        int lastMatch = afterIndex + getDelta(region) + 1;
        while (lastMatch != -1) {
            switch (region) {
                case BOLD | ITALICS_A:
                    lastMatch = sequence.indexOf("***", lastMatch);
                    break;
                case BOLD:
                    lastMatch = sequence.indexOf("**", lastMatch);
                    if (lastMatch != -1 && hasCollision(lastMatch + 1, sequence, '*')) // did we find a bold italics
                    // tag?
                    {
                        lastMatch += 3;
                        continue;
                    }
                    break;
                case ITALICS_A:
                    lastMatch = sequence.indexOf('*', lastMatch);
                    if (lastMatch != -1 && hasCollision(lastMatch, sequence, '*')) // did we find a bold tag?
                    {
                        if (hasCollision(lastMatch + 1, sequence, '*'))
                            lastMatch += 3;
                        else
                            lastMatch += 2;
                        continue;
                    }
                    break;
                case UNDERLINE:
                    lastMatch = sequence.indexOf("__", lastMatch);
                    break;
                case ITALICS_U:
                    lastMatch = sequence.indexOf('_', lastMatch);
                    if (lastMatch != -1 && hasCollision(lastMatch, sequence, '_')) // did we find an underline tag?
                    {
                        lastMatch += 2;
                        continue;
                    }
                    break;
                case SPOILER:
                    lastMatch = sequence.indexOf("||", lastMatch);
                    break;
                case BLOCK:
                    lastMatch = sequence.indexOf("```", lastMatch);
                    break;
                case MONO_TWO:
                    lastMatch = sequence.indexOf("``", lastMatch);
                    if (lastMatch != -1 && hasCollision(lastMatch + 1, sequence, '`')) // did we find a codeblock?
                    {
                        lastMatch += 3;
                        continue;
                    }
                    break;
                case MONO:
                    lastMatch = sequence.indexOf('`', lastMatch);
                    if (lastMatch != -1 && hasCollision(lastMatch, sequence, '`')) // did we find a codeblock?
                    {
                        if (hasCollision(lastMatch + 1, sequence, '`'))
                            lastMatch += 3;
                        else
                            lastMatch += 2;
                        continue;
                    }
                    break;
                case STRIKE:
                    lastMatch = sequence.indexOf("~~", lastMatch);
                    break;
                default:
                    return -1;
            }
            if (lastMatch == -1 || !doesEscape(lastMatch, sequence))
                return lastMatch;
            lastMatch++;
        }
        return -1;
    }

    @Nonnull
    private String handleRegion(int start, int end, @Nonnull String sequence, int region) {
        String resolved = sequence.substring(start, end);
        switch (region) {
            case BLOCK:
            case MONO:
            case MONO_TWO:
                return resolved;
            default:
                return new MarkdownConverter(ignored).compute(resolved);
        }
    }

    private int getDelta(int region) {
        switch (region) {
            case ESCAPED_BLOCK:
            case ESCAPED_BOLD | ITALICS_A:
            case BLOCK:
            case BOLD | ITALICS_A:
                return 3;
            case ESCAPED_MONO_TWO:
            case ESCAPED_BOLD:
            case ESCAPED_UNDERLINE:
            case ESCAPED_SPOILER:
            case ESCAPED_STRIKE:
            case MONO_TWO:
            case BOLD:
            case UNDERLINE:
            case SPOILER:
            case STRIKE:
                return 2;
            case ESCAPED_ITALICS_A:
            case ESCAPED_ITALICS_U:
            case ESCAPED_MONO:
            case ITALICS_A:
            case ITALICS_U:
            case MONO:
                return 1;
            default:
                return 0;
        }
    }

    private void applyStrategy(int region, @Nonnull String seq, @Nonnull StringBuilder builder) {
        String token = tokens.get(region);
        if (token == null)
            throw new IllegalStateException("Found illegal region for strategy CONVERT '" + region + "' with no known" +
                    " " +
                    "format token!");

        builder.append(getConverted(token))
                .append(seq)
                .append(ChatColor.RESET)
        ;
    }

    private String getConverted(String token) {
        List<ChatColor> start = new ArrayList<>();
        switch (token) {
            case "_":
            case "*":
                start.add(ChatColor.ITALIC);
                break;
            case "**":
                start.add(ChatColor.BOLD);
                break;
            case "***":
                start.add(ChatColor.BOLD);
                start.add(ChatColor.ITALIC);
            case "__":
                start.add(ChatColor.UNDERLINE);
                break;
            case "___":
                start.add(ChatColor.UNDERLINE);
                start.add(ChatColor.ITALIC);
                break;

            case "~~":
                start.add(ChatColor.STRIKETHROUGH);
                break;
        }

        return String.join("",
                start.stream().map(ChatColor::toString).collect(Collectors.toCollection(ArrayList::new)));
    }

    private boolean doesEscape(int index, @Nonnull String seq) {
        int backslashes = 0;
        for (int i = index - 1; i > -1; i--) {
            if (seq.charAt(i) != '\\')
                break;
            backslashes++;
        }
        return backslashes % 2 != 0;
    }

    private boolean isEscape(int region) {
        return (Integer.MIN_VALUE & region) != 0;
    }

    private boolean isIgnored(int nextRegion) {
        return (nextRegion & ignored) == nextRegion;
    }

    /**
     * Computes the provided input.
     * Ignores any regions specified with {@link #withIgnored(int)}.
     *
     * @param sequence The string to compute
     * @return The resulting string after applying the computation
     * @throws java.lang.IllegalArgumentException If the provided string is null
     */
    @Nonnull
    public String compute(@Nonnull String sequence) {
        Checks.notNull(sequence, "Input");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < sequence.length(); ) {
            int nextRegion = getRegion(i, sequence);
            if (nextRegion == NORMAL) {
                builder.append(sequence.charAt(i++));
                continue;
            }

            int endRegion = findEndIndex(i, nextRegion, sequence);
            if (isIgnored(nextRegion) || endRegion == -1) {
                int delta = getDelta(nextRegion);
                for (int j = 0; j < delta; j++)
                    builder.append(sequence.charAt(i++));
                continue;
            }
            int delta = getDelta(nextRegion);
            applyStrategy(nextRegion, handleRegion(i + delta, endRegion, sequence, nextRegion), builder);
            i = endRegion + delta;
        }
        return builder.toString();
    }

}
