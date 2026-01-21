package no.nav.ung.ytelse.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public interface UttakRegelVurderer {

    UttakDelResultat vurder(LocalDateTimeline<Boolean> tidslinjeTilVurdering);

}
