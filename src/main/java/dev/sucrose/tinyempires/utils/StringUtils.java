package dev.sucrose.tinyempires.utils;

import org.bukkit.ChatColor;

import java.util.List;

public class StringUtils {

    /**
     * Get slice of string array delimited by spaces
     * @param arr - String array to slice
     * @param startIndex - Index to start from (inclusive)
     * @param endIndex - Index to end at (exclusive)
     * @return slice - Array slice joined with spaces
     */
    public static String buildWordsFromArray(String[] arr, int startIndex, int endIndex) {
        StringBuilder builder = new StringBuilder();
        for (int i = startIndex; i < endIndex; i++)
            builder.append(arr[i]).append(i != endIndex - 1 ? " " : "");
        return builder.toString();
    }

    /**
     * #buildWordsFromArray but endIndex is array length by default
     * @param arr - String array to slice
     * @param startIndex - Index to start from (inclusive)
     * @return slice - Array slice joined with spaces
     */
    public static String buildWordsFromArray(String[] arr, int startIndex) {
        return buildWordsFromArray(arr, startIndex, arr.length);
    }

    public static String worldDirToName(String world) {
        switch (world) {
            case "world": return "Overworld";
            case "world_the_end": return "End";
            case "world_nether": return "Nether";
            case "chess": return "Chess";
        }
        return null;
    }

    public static String stringListToGrammaticalList(List<String> strings, ChatColor spacerColor) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < strings.size() - 1; i++)
            stringBuilder.append(ChatColor.BOLD)
                .append(strings.get(i))
                .append(spacerColor)
                .append(i == strings.size() - 2 ? " " : ", ");
        return stringBuilder
            .append(strings.size() > 1 ? "and" : "")
            .append(strings.get(strings.size() - 1))
            .toString();
    }

    public static String sanitizeDiscordText(String text) {
        return text.replace("`", "\u200B`")
            .replace("*", "\u200B*")
            .replace("_", "\u200B_");
    }

}
