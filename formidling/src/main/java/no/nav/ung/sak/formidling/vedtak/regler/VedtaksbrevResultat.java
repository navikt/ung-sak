package no.nav.ung.sak.formidling.vedtak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.formidling.vedtak.DetaljertResultat;

import java.util.List;

public record VedtaksbrevResultat(
    LocalDateTimeline<DetaljertResultat> detaljertResultatTimeline,
    List<VedtaksbrevRegelResultat> vedtaksbrevRegelResultater) {

}

