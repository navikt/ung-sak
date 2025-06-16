package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;

import java.util.Map;

/**
 * Innvilger hele perioden
 */
public class InnvilgHelePeriodenVurderer implements UttakRegelVurderer {
    @Override
    public UttakDelResultat vurder(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        return new UttakDelResultat(
            tidslinjeTilVurdering.mapValue(it -> UttakResultat.forInnvilgelse()),
            LocalDateTimeline.empty(),
            Map.of()
        );
    }
}
