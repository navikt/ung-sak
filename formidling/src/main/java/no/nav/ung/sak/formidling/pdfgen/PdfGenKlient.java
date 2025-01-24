package no.nav.ung.sak.formidling.pdfgen;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.slf4j.Slf4jLogger;
import com.openhtmltopdf.util.XRLog;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.pdfgen.core.Environment;
import no.nav.pdfgen.core.PDFGenCore;
import no.nav.pdfgen.core.PDFGenResource;
import no.nav.pdfgen.core.pdf.CreateHtmlKt;
import no.nav.pdfgen.core.pdf.CreatePdfKt;
import no.nav.ung.sak.formidling.template.TemplateInput;

@ApplicationScoped
public class PdfGenKlient {
    private final ObjectMapper pdfgenObjectMapper;
    //Brukes for test da pdfgenerering er tregt.
    private final boolean ignorePdf;

    private final Logger log = LoggerFactory.getLogger(PdfGenKlient.class);

    @Inject
    public PdfGenKlient() {
        this(false);
    }

    public PdfGenKlient(Boolean ignorePdf) {
        this.ignorePdf = Objects.requireNonNullElse(ignorePdf, false);
        System.setProperty("sun2d.cmm", "sun2d.cmm.kcms.KcmsServiceProvider");
        VeraGreenfieldFoundryProvider.initialise();
        XRLog.setLoggerImpl(new Slf4jLogger());
        Environment initialEnvironment = new Environment(
            Collections.emptyMap(),
            new PDFGenResource(getResource("templates/")),
            new PDFGenResource(getResource("resources/")),
            new PDFGenResource(getResource("fonts/")),
            new PDFGenResource("")
        );
        PDFGenCore.Companion.init(initialEnvironment);
        pdfgenObjectMapper = PdfGenObjectMapperConfig.lag();

    }

    private Path getResource(String relativePath) {
        String faktiskPath = "pdfgen/%s".formatted(relativePath);
        Path path = Path.of(faktiskPath);
        if (Files.exists(path)) {
            // Finnes i rotmappen til der appen kjører fra, typisk fra docker
            return path;
        }

        log.info("Fant ikke pdfgen-ressurser på {}. Prøver å hente fra classpath (resource) til modulen. " +
                 "Bør bare skje for test", faktiskPath);

        //Fantes ikke, fallback til resourcemappen til modulen, typisk for tester
        URL resource = getClass().getClassLoader().getResource(faktiskPath);
        Objects.requireNonNull(resource, "Fant ingen resource på  " + faktiskPath);

        try {
            return Path.of(resource.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    public PdfGenDokument lagDokument(TemplateInput payload) {
        JsonNode templateData = pdfgenObjectMapper.convertValue(payload.templateDto(), JsonNode.class);
        return lagDokument(payload.templateType().getPath(), payload.templateType().getDir(), templateData);
    }

    private PdfGenDokument lagDokument(String templateNavn, String dir, JsonNode payload) {
        String html = CreateHtmlKt.createHtml(templateNavn, dir, payload);
        Objects.requireNonNull(html);
        if (ignorePdf) {
            return new PdfGenDokument(null, html);
        }
        var pdfStartInstant = Instant.now();
        byte[] pdfa = CreatePdfKt.createPDFA(html);
        log.info("Tid pdfgenerering: {} ms", Duration.between(pdfStartInstant, Instant.now()).toMillis());
        return new PdfGenDokument(pdfa, html);
    }


}
