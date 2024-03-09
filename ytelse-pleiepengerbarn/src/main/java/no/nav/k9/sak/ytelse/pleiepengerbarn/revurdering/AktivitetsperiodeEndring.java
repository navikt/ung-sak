package no.nav.k9.sak.ytelse.pleiepengerbarn.revurdering;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.registerendringer.Endringstype;

public record AktivitetsperiodeEndring(AktivitetsIdentifikator identifikator,
                                       LocalDateTimeline<Endringstype> endringstidslinje) {
}

