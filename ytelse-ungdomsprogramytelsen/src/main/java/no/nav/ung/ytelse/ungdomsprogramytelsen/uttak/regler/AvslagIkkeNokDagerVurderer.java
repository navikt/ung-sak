package no.nav.ung.ytelse.ungdomsprogramytelsen.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FinnForbrukteDager;

import java.util.Map;

/**
 * VURDERING AV IKKE NOK DAGER: Perioder etter maks antall dager er brukt opp får 0% utbetaling
 */
public class AvslagIkkeNokDagerVurderer implements UttakRegelVurderer {

    private final LocalDateTimeline<Boolean> ungdomsprogramtidslinje;
    private final boolean harForlengetPeriode;

    public AvslagIkkeNokDagerVurderer(LocalDateTimeline<Boolean> ungdomsprogramtidslinje, boolean harForlengetPeriode) {
        this.ungdomsprogramtidslinje = ungdomsprogramtidslinje;
        this.harForlengetPeriode = harForlengetPeriode;
    }

    @Override
    public UttakDelResultat vurder(LocalDateTimeline<Boolean> tidslinjeTilVurdering) {
        final var vurderAntallDagerResultat = FinnForbrukteDager.finnForbrukteDager(ungdomsprogramtidslinje, harForlengetPeriode);
        return finnUttaksperioderAvslagEtterDød(tidslinjeTilVurdering, vurderAntallDagerResultat.tidslinjeNokDager());
    }

    private UttakDelResultat finnUttaksperioderAvslagEtterDød(LocalDateTimeline<Boolean> tidslinjeTilVurdering, LocalDateTimeline<Boolean> nokDagerTidslinje) {
        final var ikkeNokDagerTidslinje = tidslinjeTilVurdering.disjoint(nokDagerTidslinje);
        return new UttakDelResultat(ikkeNokDagerTidslinje.mapValue(it -> UttakResultat.forAvslag(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER)),
            LocalDateTimeline.empty(),
            Map.of("perioderNokDager", nokDagerTidslinje.getLocalDateIntervals().toString(),
                "perioderUtenNokDager", tidslinjeTilVurdering.getLocalDateIntervals().toString()));
    }

}
