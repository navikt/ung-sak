package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

public interface UttakRegelVurderer {

    UttakDelResultat vurder(LocalDateTimeline<Boolean> tidslinjeTilVurdering);

}
