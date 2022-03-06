package games.scorpio.mongo.util;

import java.io.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FileUtil {

    public static void write(String str, File file) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(str);
            writer.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void read(File file, Consumer<BufferedReader> consumer) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            consumer.accept(reader);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void copy(File copy, File copyTo) {
        read(copy, reader -> write(reader.lines().collect(Collectors.joining("\n")), copyTo));
    }

}
