package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor;


import java.util.List;

/**
 * HTML seksjoner for breveditor
 * @param original - orignal versjon av brevet
 * @param redigert - redigert versjon av brevet. Null hvis ingenting er redigert.
 * @param tidligereRedigert - redigert versjon før evt. tilbakehopp.
 */
public record VedtaksbrevEditorResponse(
    List<VedtaksbrevSeksjon> original,
    List<VedtaksbrevSeksjon> redigert,
    List<VedtaksbrevSeksjon> tidligereRedigert
) {

}




