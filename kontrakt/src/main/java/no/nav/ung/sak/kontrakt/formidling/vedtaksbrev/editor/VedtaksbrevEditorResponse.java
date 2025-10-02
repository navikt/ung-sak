package no.nav.ung.sak.kontrakt.formidling.vedtaksbrev.editor;


import java.util.List;

/**
 * Response objekt - skal ikke sendes inn
 *
 */
public record VedtaksbrevEditorResponse(
    List<VedtaksbrevSeksjon> original
) {

}




