package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VURDERING AV OPPBRUKT KVOTE: Perioder etter kvote er brukt opp får 0% utbetaling
 */
public class AvslagIkkeNokDagerVurderer implements UttakRegelVurderer {

    private final LocalDateTimeline<Boolean> perioderTilVurdering;

    public AvslagIkkeNokDagerVurderer(LocalDateTimeline<Boolean> perioderTilVurdering) {
        this.perioderTilVurdering = perioderTilVurdering;
    }

    @Override
    public UttakDelResultat vurder() {
        return finnUttaksperioderAvslagEtterDød();
    }


    private UttakDelResultat finnUttaksperioderAvslagEtterDød() {
        final var uttakPeriodeAvslåttEtterSøkersDød = mapTilUttakPerioderAvslåttGrunnetSøkersDødfall(perioderTilVurdering);
        return new UttakDelResultat(uttakPeriodeAvslåttEtterSøkersDød, LocalDateTimeline.empty(),
            Map.of("perioderUtenNokDager", perioderTilVurdering.getLocalDateIntervals().toString()));
    }

    private static List<UngdomsytelseUttakPeriode> mapTilUttakPerioderAvslåttGrunnetSøkersDødfall(LocalDateTimeline<Boolean> tidslinjeUtenNokDager) {
        return tidslinjeUtenNokDager
            .getLocalDateIntervals()
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER, DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new));
    }

}
