package no.nav.ung.sak.formidling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PdfUtils {
    public static void lagrePdf(byte[] data, String filename) {
        Path directory = null;
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
