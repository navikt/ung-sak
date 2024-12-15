package no.nav.ung.sak.formidling.pdfgen;

import java.util.Collections;
import java.util.Objects;

import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.pdfgen.core.Environment;
import no.nav.pdfgen.core.PDFGenCore;
import no.nav.pdfgen.core.PDFGenResource;
import no.nav.pdfgen.core.pdf.CreateHtmlKt;
import no.nav.pdfgen.core.pdf.CreatePdfKt;
import no.nav.ung.sak.formidling.template.TemplateInput;

@ApplicationScoped
public class PdfGen {
    private static String RESOURCE_TEMPLATE = "pdfgen/%s";
    private final ObjectMapper pdfgenObjectMapper;
    //Brukes for test da pdfgenerering er tregt.
    private boolean ignorePdf;


    @Inject
    public PdfGen() {
        this(false);
    }
    public PdfGen(Boolean ignorePdf) {
        this.ignorePdf = Objects.requireNonNullElse(ignorePdf, false);
        System.setProperty("sun2d.cmm", "sun2d.cmm.kcms.KcmsServiceProvider");
        VeraGreenfieldFoundryProvider.initialise();
        Environment initialEnvironment = new Environment(
            Collections.emptyMap(),
            new PDFGenResource(RESOURCE_TEMPLATE.formatted("templates/")),
            new PDFGenResource(RESOURCE_TEMPLATE.formatted("resources/")),
            new PDFGenResource(RESOURCE_TEMPLATE.formatted("fonts/")),
            new PDFGenResource(RESOURCE_TEMPLATE.formatted("data/"))
        );
        PDFGenCore.Companion.init(initialEnvironment);
        pdfgenObjectMapper = PdfGenObjectMapper.lag();

    }


    public PdfGenDokument lagDokument(TemplateInput payload) {
        JsonNode templateData = pdfgenObjectMapper.convertValue(payload.templateData(), JsonNode.class);
        return lagDokument(payload.templateType().getPath(), payload.templateType().getDir(), templateData);
    }

    private PdfGenDokument lagDokument(String templateNavn, String dir, JsonNode payload) {
        String html = CreateHtmlKt.createHtml(templateNavn, dir, payload);
        Objects.requireNonNull(html);
        if (ignorePdf) {
            return new PdfGenDokument(null, html);
        }
        byte[] pdfa = CreatePdfKt.createPDFA(html);
        return new PdfGenDokument(pdfa, html);
    }


}
