package no.nav.ung.sak.formidling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class BrevUtils {

    static DateTimeFormatter norwegianFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.forLanguageTag("no-NO"));

    public static String brevDatoString(LocalDate date) {
        return date.format(norwegianFormatter);
    }

    public static void lagrePdf(byte[] data, String filename) {
        Path directory;
        try {
            directory = Files.createDirectories(Paths.get("pdfresultater"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File file = directory.resolve(filename + ".pdf").toFile();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
