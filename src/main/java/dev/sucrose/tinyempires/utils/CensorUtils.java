package dev.sucrose.tinyempires.utils;

import com.mongodb.client.MongoCollection;
import dev.sucrose.tinyempires.TinyEmpires;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class CensorUtils {

    private static final List<String> censors = new ArrayList<>();
    private static final MongoCollection<Document> collection =
        TinyEmpires.getDatabase().getCollection("censored_curses");

    static {
        fetchCensors();
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

    private static String curseToHashtags(String curse) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < curse.length(); i++)
            stringBuilder.append('#');
        return stringBuilder.toString();
    }

    private static String getCurseCensorRegex(String curse) {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < curse.length(); i++)
            stringBuilder
                .append("(")
                .append(curse.charAt(i))
                .append("|")
                .append(String.valueOf(curse.charAt(i)).toUpperCase())
                .append(")").append(
                    i == curse.length() - 1
                        ? ""
                        : " *");
        return stringBuilder.toString();
    }

    public static String censorCurses(String message) {
        String newMessage = message;
        for (final String censor : censors) {
            newMessage = newMessage
                .replaceAll(
                    getCurseCensorRegex(censor),
                    curseToHashtags(censor)
                );
        }
        return newMessage;
    }

}
