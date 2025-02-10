package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FinnForbrukteDager;

import java.util.Map;

/**
 * VURDERING AV OPPBRUKT KVOTE: Perioder etter kvote er brukt opp får 0% utbetaling
 */
public class AvslagIkkeNokDagerVurderer implements UttakRegelVurderer {

    private final LocalDateTimeline<Boolean> ungdomsprogramtidslinje;

    public AvslagIkkeNokDagerVurderer(LocalDateTimeline<Boolean> ungdomsprogramtidslinje) {
        this.ungdomsprogramtidslinje = ungdomsprogramtidslinje;
    }

    @Override
    public UttakDelResultat vurder(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        final var vurderAntallDagerResultat = FinnForbrukteDager.finnForbrukteDager(ungdomsprogramtidslinje);
        return finnUttaksperioderAvslagEtterDød(tidslinjeTilVurdering, vurderAntallDagerResultat.tidslinjeNokDager());
    }

    private UttakDelResultat finnUttaksperioderAvslagEtterDød(LocalDateTimeline<Boolean> tidslinjeTilVurdering, LocalDateTimeline<Boolean> nokDagerTidslinje) {
        final var ikkeNokDagerTidslinje = tidslinjeTilVurdering.disjoint(nokDagerTidslinje);
        return new UttakDelResultat(ikkeNokDagerTidslinje.mapValue(it -> UttakAvslagResultat.medÅrsak(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER)),
            LocalDateTimeline.empty(),
            Map.of("perioderNokDager", nokDagerTidslinje.getLocalDateIntervals().toString(),
                "perioderUtenNokDager", tidslinjeTilVurdering.getLocalDateIntervals().toString()));
    }

}
