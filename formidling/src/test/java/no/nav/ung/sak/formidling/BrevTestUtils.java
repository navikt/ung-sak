package no.nav.ung.sak.formidling;

import jakarta.persistence.EntityManager;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.test.util.UngTestRepositories;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeVisitor;
import org.junit.jupiter.api.TestInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class BrevTestUtils {

    static final DateTimeFormatter norwegianFormatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.forLanguageTag("no-NO"));

    public static String brevDatoString(LocalDate date) {
        return date.format(norwegianFormatter);
    }

    public static UngTestRepositories lagAlleUngTestRepositories(EntityManager entityManager) {
        return UngTestRepositories.lagAlleUngTestRepositoriesOgAbakusTjeneste(entityManager, new AbakusInMemoryInntektArbeidYtelseTjeneste());
    }

    public static void lagrePdf(GenerertBrev generertBrev, TestInfo testInfo) {
        byte[] pdf = generertBrev.dokument().pdf();
        String filnavn = bestemFilnavn(generertBrev, testInfo);
        lagre(pdf, filnavn, "pdf");
    }

    public static void lagreHtml(GenerertBrev generertBrev, TestInfo testInfo) {
        String html = generertBrev.dokument().html();
        String filnavn = bestemFilnavn(generertBrev, testInfo);
        lagre(html.getBytes(StandardCharsets.UTF_8), filnavn, "html");
    }

    private static String bestemFilnavn(GenerertBrev generertBrev, TestInfo testInfo) {
        return "%s-%s"
            .formatted(
                generertBrev.templateType().name(),
                testInfo.getTestMethod().map(Method::getName).orElse("ukjentMetode"));
    }

    private static void lagre(byte[] data, String filename, String filtype) {
        Path directory;
        try {
            directory = Files.createDirectories(Paths.get("pdfresultater"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        File file = directory.resolve(filename + "." + filtype).toFile();

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String htmlToPlainText(String html) {
        return Jsoup.parse(html).text().replaceAll("(?m)^\\s*$\\n", "");
    }

    public static String trimmedHtml(String actual) {
        // Use Jsoup to parse and clean the HTML
        Document document = Jsoup.parse(actual);

        // Remove undesired elements
        document.select("style, head, img").remove();
        document.traverse(new NodeVisitor() {

            @Override
            public void head(@NotNull Node node, int depth) {
                if (node.nodeName().equals("#comment")) {
                    node.remove();
                }
            }

            @Override
            public void tail(@NotNull Node node, int depth) {
                // Do nothing on tail visit
            }
        });

        // Extract cleaned HTML as a string
        return document.body().html().replaceAll("(?m)^\\s*$\\n", "");
    }

}
