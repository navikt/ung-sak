package no.nav.ung.sak.formidling.innhold;

import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;

import java.util.Set;

/**
 * Validerer at brevet er gyldig før pdf generering
 * Se også {@link no.nav.ung.sak.behandlingslager.formidling.XhtmlBrevRenser}
 */
public class ManueltVedtaksbrevValidator {
    public static void valider(String redigertBrevHtml) {
        if (redigertBrevHtml == null || redigertBrevHtml.isBlank()) {
            throw new IllegalStateException("Ingen tekst oppgitt");
        }

        var parsedHtml = Jsoup.parse(redigertBrevHtml, Parser.htmlParser());

        if (parsedHtml.body().text().trim().isEmpty()) {
            throw new IllegalStateException("Manuelt brev kan ikke være tom");
        }

        var forsteTagg = parsedHtml.body().firstElementChild();
        Set<String> gyldigeOverskriftTagger = Set.of("h1", "h2", "h3", "h4", "h5", "h6");

        if (forsteTagg == null || !gyldigeOverskriftTagger.contains(forsteTagg.tagName())) {
            throw new IllegalStateException("Manuelt brev må ha overskift som første element, men fant html-tagg=" +
                (forsteTagg != null ? forsteTagg.tagName() : "ingen"));
        }

        if (forsteTagg.text().trim().isEmpty()) {
            throw new IllegalStateException("Manuelt brev har tom overskrift");
        }

        if (redigertBrevHtml.contains(TomVedtaksbrevInnholdBygger.TOM_VEDTAKSBREV_HTML_OVERSKRIFT)) {
            throw new IllegalStateException("Manuelt brev innholder preutfylt overskrift! ");
        }

        if (redigertBrevHtml.contains(TomVedtaksbrevInnholdBygger.TOM_VEDTAKSBREV_HTML_BRØDTEKST)) {
            throw new IllegalStateException("Manuelt brev innholder preutfylt brødtekst!");
        }
    }
}
