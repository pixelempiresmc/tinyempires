package dev.sucrose.tinyempires.utils;

import com.mongodb.client.MongoCollection;
import dev.sucrose.tinyempires.TinyEmpires;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CensorUtils {

    private static final List<String> censors = new ArrayList<>();
    private static final MongoCollection<Document> collection =
        TinyEmpires.getDatabase().getCollection("censored_curses");
    private static final Pattern SPECIAL_REGEX_CHARS = Pattern.compile("[{}()\\[\\].+*?^$\\\\|]");

    static {
        fetchCensors();
    }

    private static String escapeSpecialRegexChars(String str) {
        // escapes special regex characters in a string to make it safe for pattern-matching
        return SPECIAL_REGEX_CHARS.matcher(str).replaceAll("\\\\$0");
    }

    public static List<String> getCensors() {
        return censors;
    }

    public static void addCensor(String censor) {
        censors.add(censor);
        collection.insertOne(new Document("text", censor));
    }

    public static void removeCensor(String censor) {
        censors.remove(censor);
        collection.deleteOne(new Document("text", censor));
    }

    public static void fetchCensors() {
        collection.find().forEach(curseDoc -> censors.add(curseDoc.getString("text")));
    }

    private static String curseToAsterisks(String curse) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < curse.length(); i++)
            stringBuilder.append('*');
        return stringBuilder.toString();
    }

    private static String getCurseCensorRegex(String curse) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < curse.length() - 1; i++)
            stringBuilder.append(curse.charAt(i)).append(" *");
        return curse.replaceAll(stringBuilder.toString(), curseToAsterisks(curse));
    }

    public static String censorCurses(String message) {
        String newMessage = message;
        for (final String censor : censors) {
            newMessage = escapeSpecialRegexChars(newMessage)
                .replaceAll(
                    getCurseCensorRegex(censor),
                    curseToAsterisks(censor)
                );
        }
        return newMessage;
    }

}
