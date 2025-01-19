package no.nav.ung.sak.formidling;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.junit.jupiter.api.TestInfo;

import no.nav.ung.sak.formidling.domene.GenerertBrev;

public class BrevUtils {

    static DateTimeFormatter norwegianFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.forLanguageTag("no-NO"));

    public static String brevDatoString(LocalDate date) {
        return date.format(norwegianFormatter);
    }

    public static void lagrePdf(GenerertBrev generertBrev, TestInfo testInfo) {
        byte[] pdf = generertBrev.dokument().pdf();
        String filnavn = "%s-%s"
            .formatted(
                generertBrev.templateType().name(),
                testInfo.getTestMethod().map(Method::getName).orElse("ukjentMetode"));
        lagrePdf(pdf, filnavn);
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
