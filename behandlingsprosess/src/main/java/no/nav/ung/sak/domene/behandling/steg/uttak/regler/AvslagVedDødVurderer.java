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
 * VURDERING AV AVSLAG ETTER DØD: Perioder med nok dager etter søkers dødsfall får 0% i utbetaling
 */
public class AvslagVedDødVurderer implements UttakRegelVurderer {


    private final LocalDateTimeline<Boolean> perioderTilVurdering;
    private final LocalDateTimeline<Boolean> levendeBrukerTidslinje;

    public AvslagVedDødVurderer(LocalDateTimeline<Boolean> perioderTilVurdering, LocalDateTimeline<Boolean> levendeBrukerTidslinje) {
        this.perioderTilVurdering = perioderTilVurdering;
        this.levendeBrukerTidslinje = levendeBrukerTidslinje;
    }

    @Override
    public UttakDelResultat vurder() {
        return finnUttaksperioderAvslagEtterDød();
    }


    private UttakDelResultat finnUttaksperioderAvslagEtterDød() {
        var avslåttEtterSøkersDødTidslinje = perioderTilVurdering.disjoint(levendeBrukerTidslinje);
        final var uttakPeriodeAvslåttEtterSøkersDød = mapTilUttakPerioderAvslåttGrunnetSøkersDødfall(avslåttEtterSøkersDødTidslinje);
        return new UttakDelResultat(uttakPeriodeAvslåttEtterSøkersDød, perioderTilVurdering.disjoint(avslåttEtterSøkersDødTidslinje),
            Map.of("avslåttEtterSøkersDødTidslinje", avslåttEtterSøkersDødTidslinje.getLocalDateIntervals().toString()));
    }

    private static List<UngdomsytelseUttakPeriode> mapTilUttakPerioderAvslåttGrunnetSøkersDødfall(LocalDateTimeline<Boolean> avslåttEtterSøkersDødTidslinje) {
        return avslåttEtterSøkersDødTidslinje
            .getLocalDateIntervals()
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL, DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new));
    }

}
