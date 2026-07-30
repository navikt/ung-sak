package no.nav.ung.sak.formidling.pdfgen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.util.XRLog;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.felles.log.trace.OpentelemetrySpanWrapper;
import no.nav.pdfgen.core.Environment;
import no.nav.pdfgen.core.PDFGenCore;
import no.nav.pdfgen.core.PDFGenResource;
import no.nav.pdfgen.core.pdf.CreateHtmlKt;
import no.nav.pdfgen.core.pdf.CreatePdfKt;
import no.nav.ung.sak.formidling.BrevGenereringSemafor;
import no.nav.ung.sak.formidling.template.TemplateInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;

@ApplicationScoped
public class PdfGenKlient {
    private final ObjectMapper pdfgenObjectMapper;

    private final Logger log = LoggerFactory.getLogger(PdfGenKlient.class);

    public PdfGenKlient() {
        System.setProperty("sun2d.cmm", "sun2d.cmm.kcms.KcmsServiceProvider");
        VeraGreenfieldFoundryProvider.initialise();
        XRLog.setLoggerImpl(new Slf4jLogger());
        Environment initialEnvironment = new Environment(
            Collections.emptyMap(),
            new PDFGenResource(getResource("templates")),
            new PDFGenResource(getResource("resources")),
            new PDFGenResource(getResource("fonts")),
            new PDFGenResource("") //denne trengs ikke, er er fordi PDFGenCore krever 4 argumenter
        );
        PDFGenCore.Companion.init(initialEnvironment);
        pdfgenObjectMapper = PdfGenObjectMapperConfig.lag();

    }

    private Path getResource(String relativePath) {
        String classpathSti = "pdfgen/%s".formatted(relativePath);

        // 1. Sjekk om filene finnes på filsystemet relativt til working directory (typisk Docker: /app/pdfgen/...)
        Path path = Path.of(classpathSti);
        if (Files.exists(path)) {
            return path;
        }

        // 2. sjekk classpath - brukes av IDE
        URL resource = getClass().getClassLoader().getResource(classpathSti);
        if (resource != null) {
            try {
                log.info("Fant ikke pdfgen-ressurser på filsystemet på {}. Fant ressurs på classpath ({}). " +
                    "Bør bare skje for test via IDE.", path.toAbsolutePath(), resource);
                return Path.of(resource.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }


        // 3. Sjekk target/pdfgen/  - brukes av maven tester
        Path targetPath = Path.of("target", classpathSti);
        if (Files.exists(targetPath)) {
            return targetPath;
        }

        throw new IllegalArgumentException(
            "Fant ikke pdfgen-ressurser. Forsøkte relativePath='%s', classpathSti='%s', targetPath='%s'"
                .formatted(relativePath, classpathSti, targetPath)
        );

    }

    @WithSpan
    public PdfGenDokument lagDokument(TemplateInput payload, boolean kunHtml) {
        return BrevGenereringSemafor.begrensetParallellitet(() -> doLagDokument(payload,  kunHtml));
    }

    @WithSpan
    public PdfGenDokument doLagDokument(TemplateInput payload, boolean kunHtml) {
        JsonNode templateData = pdfgenObjectMapper.convertValue(payload.templateDto(), JsonNode.class);
        return lagDokument(payload.templateType().getPath(), payload.templateType().getDir(), templateData, kunHtml);
    }


    @WithSpan
    public PdfGenDokument lagDokument(String templateNavn, String dir, Object data, boolean kunHtml) {
        JsonNode templateData = pdfgenObjectMapper.convertValue(data, JsonNode.class);
        return lagDokument(templateNavn, dir, templateData, kunHtml);
    }

    private PdfGenDokument lagDokument(String templateNavn, String dir, JsonNode payload, boolean kunHtml) {
        String html = OpentelemetrySpanWrapper.forApplikasjon().span("pdfgen.lagDokument.createHtml",
            span -> span.setAttribute("templateNavn", templateNavn).setAttribute("templateDir", dir),
            () -> CreateHtmlKt.createHtml(templateNavn, dir, payload)
        );
        Objects.requireNonNull(html);
        if (kunHtml) {
            return new PdfGenDokument(null, html);
        }
        var pdfStartInstant = Instant.now();
        byte[] pdfa = OpentelemetrySpanWrapper.forApplikasjon().span("pdfgen.lagDokument.creatPDFA",
            span -> span.setAttribute("templateNavn", templateNavn).setAttribute("templateDir", dir),
            () -> CreatePdfKt.createPDFA(html)
        );
        log.info("Tid pdfgenerering: {} ms", Duration.between(pdfStartInstant, Instant.now()).toMillis());
        return new PdfGenDokument(pdfa, html);
    }

}
