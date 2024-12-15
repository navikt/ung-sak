package no.nav.ung.sak.formidling.pdfgen;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Objects;

import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.pdfgen.core.Environment;
import no.nav.pdfgen.core.PDFGenCore;
import no.nav.pdfgen.core.PDFGenResource;
import no.nav.pdfgen.core.pdf.CreateHtmlKt;
import no.nav.pdfgen.core.pdf.CreatePdfKt;
import no.nav.ung.sak.formidling.template.TemplateInput;
import no.nav.ung.sak.formidling.template.dto.TemplateData;

@ApplicationScoped
public class PdfGen {
    private static String RESOURCE_TEMPLATE = "pdfgen/%s";
    private ObjectMapper pdfgenObjectMapper;

    public PdfGen() {
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


    public byte[] lagPdf(String templateNavn, String dir, JsonNode payload) {
        String html = CreateHtmlKt.createHtml(templateNavn, dir, payload);
        Objects.requireNonNull(html);
        return CreatePdfKt.createPDFA(html);
    }

    public byte[] lagPdf(TemplateInput payload) {
        JsonNode templateData = pdfgenObjectMapper.convertValue(payload, JsonNode.class);
        return lagPdf(payload.templateType().getPath(), payload.templateType().getDir(), templateData);
    }


}
