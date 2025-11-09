package no.nav.ung.sak.formidling;

import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjon;
import no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor.VedtaksbrevSeksjonType;

import java.util.List;

public class BrevXhtmlTilSeksjonKonverter {
    public static List<VedtaksbrevSeksjon> konverter(String html) {
        return List.of(
            new VedtaksbrevSeksjon(VedtaksbrevSeksjonType.STYLE, "<style></style>"),
            new VedtaksbrevSeksjon(VedtaksbrevSeksjonType.STATISK, "Til XXXXXX"),
            new VedtaksbrevSeksjon(VedtaksbrevSeksjonType.REDIGERBAR, "<h1>Overskrift</h1>"),
            new VedtaksbrevSeksjon(VedtaksbrevSeksjonType.STATISK, "<h2>Du kan klage</h2>")
        );
    }
}
