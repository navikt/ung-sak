package no.nav.ung.sak.formidling.vedtak;

import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjon;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjonType;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

public class BrevXhtmlTilSeksjonKonverter {
    public static List<VedtaksbrevSeksjon> konverter(String html) {
        List<VedtaksbrevSeksjon> seksjoner = new ArrayList<>();

        // Parse HTML med Jsoup
        Document doc = Jsoup.parse(html);

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
