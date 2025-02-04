package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

import java.util.Map;

/**
 * VURDERING AV AVSLAG ETTER DØD: Perioder med nok dager etter søkers dødsfall får 0% i utbetaling
 */
public class AvslagVedDødVurderer implements UttakRegelVurderer {


    private final LocalDateTimeline<Boolean> levendeBrukerTidslinje;

    public AvslagVedDødVurderer(LocalDateTimeline<Boolean> levendeBrukerTidslinje) {
        this.levendeBrukerTidslinje = levendeBrukerTidslinje;
    }

    @Override
    public UttakDelResultat vurder(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        return finnUttaksperioderAvslagEtterDød(tidslinjeTilVurdering);
    }

    private UttakDelResultat finnUttaksperioderAvslagEtterDød(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        var avslåttEtterSøkersDødTidslinje = tidslinjeTilVurdering.disjoint(levendeBrukerTidslinje);
        return new UttakDelResultat(
            avslåttEtterSøkersDødTidslinje.mapValue(it -> UttakResultat.forAvslag(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL)),
            tidslinjeTilVurdering.disjoint(avslåttEtterSøkersDødTidslinje),
            Map.of("avslåttEtterSøkersDødTidslinje", avslåttEtterSøkersDødTidslinje.getLocalDateIntervals().toString()));
    }

}
