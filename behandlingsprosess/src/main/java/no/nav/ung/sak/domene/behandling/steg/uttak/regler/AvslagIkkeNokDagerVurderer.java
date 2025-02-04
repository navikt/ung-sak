package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;

import java.math.BigDecimal;
import java.util.Map;

/**
 * VURDERING AV OPPBRUKT KVOTE: Perioder etter kvote er brukt opp får 0% utbetaling
 */
public class AvslagIkkeNokDagerVurderer implements UttakRegelVurderer {


    private final LocalDateTimeline<Boolean> nokDagerTidslinje;

    public AvslagIkkeNokDagerVurderer(LocalDateTimeline<Boolean> nokDagerTidslinje) {
        this.nokDagerTidslinje = nokDagerTidslinje;
    }

    @Override
    public UttakDelResultat vurder(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        return finnUttaksperioderAvslagEtterDød(tidslinjeTilVurdering);
    }

    private UttakDelResultat finnUttaksperioderAvslagEtterDød(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        final var ikkeNokDagerTidslinje = tidslinjeTilVurdering.disjoint(nokDagerTidslinje);
        return new UttakDelResultat(ikkeNokDagerTidslinje.mapValue(it -> UttakResultat.forAvslag(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER)),
            LocalDateTimeline.empty(),
            Map.of("perioderNokDager", nokDagerTidslinje.getLocalDateIntervals().toString(),
                "perioderUtenNokDager", tidslinjeTilVurdering.getLocalDateIntervals().toString()));
    }

}
