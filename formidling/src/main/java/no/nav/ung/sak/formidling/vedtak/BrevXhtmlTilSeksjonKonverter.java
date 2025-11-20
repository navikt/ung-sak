package no.nav.ung.sak.formidling.vedtak;

import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjon;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjonType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class BrevXhtmlTilSeksjonKonverter {
    private static final Safelist SAFELIST = Safelist.none()
        .addTags(
            "div",
            "p",
            "h1",
            "h2",
            "a",
            "span",
            "section",
            "header",
            "footer",
            "style",
            "table",
            "tr",
            "td"

        )
        .addAttributes(":all", "class", "id")
        .addAttributes("div", "data-hidden", "data-editable")
        .addAttributes("a", "href", "title")
        .addAttributes("span", "class")
        .addAttributes(":all", "style")
        .addProtocols("a", "href", "http", "https");



    public static List<VedtaksbrevSeksjon> konverter(String html) {
        List<VedtaksbrevSeksjon> seksjoner = new ArrayList<>();

        // Parse HTML med Jsoup
        String clean = Jsoup.clean(html, "", SAFELIST);
        Document doc = Jsoup.parse(clean);
        doc.outputSettings(new Document.OutputSettings()
            .syntax(Document.OutputSettings.Syntax.xml)
            .charset(StandardCharsets.UTF_8)
            .prettyPrint(false));

        // Del 1: Style - Trekk ut style taggen
        Element style = doc.selectFirst("style");
        if (style == null) {
            throw new IllegalArgumentException("Fant ingen styleelement");
        }

        seksjoner.add(new VedtaksbrevSeksjon(VedtaksbrevSeksjonType.STYLE, style.outerHtml()));

        Element header = doc.selectFirst("header");
        if (header == null) {
            throw new IllegalArgumentException("Fant ingen headerelement");
        }
        header.select("#nav_logo_container").remove();
        seksjoner.add(new VedtaksbrevSeksjon(VedtaksbrevSeksjonType.STATISK, header.outerHtml()));

        Element editableDiv = doc.body().selectFirst("div[data-editable]");
        if (editableDiv == null) {
            throw new IllegalArgumentException("Fant ingen <div data-editable=...> element");
        }
        seksjoner.add(new VedtaksbrevSeksjon(VedtaksbrevSeksjonType.REDIGERBAR, editableDiv.html()));

        Elements footer = editableDiv.nextElementSiblings();
        seksjoner.add(new VedtaksbrevSeksjon(VedtaksbrevSeksjonType.STATISK, footer.outerHtml()));

        return seksjoner;
    }

}
