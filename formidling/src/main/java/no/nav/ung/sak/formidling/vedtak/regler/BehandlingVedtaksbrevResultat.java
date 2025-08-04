package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.formidling.vedtak.resultat.DetaljertResultat;

import java.util.List;

/**
 * Vedtaksbrevresultat for hele behandlingen. Vet om det er flere vedtaksbrev.
 */
public record BehandlingVedtaksbrevResultat(
    boolean harBrev,
    LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
    List<VedtaksbrevResultat> vedtaksbrevResultater) {

}

