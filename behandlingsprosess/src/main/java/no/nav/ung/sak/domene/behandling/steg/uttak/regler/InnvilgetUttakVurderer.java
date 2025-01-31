package no.nav.ung.sak.domene.behandling.steg.uttak.regler;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.ungdomsprogram.forbruktedager.FinnForbrukteDager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * VURDERING AV PERIODER MED NOK DAGER: Perioder med nok dager får 100% utbetaling dersom det ikke er rapportert inntekt i perioden.
 * <p>
 * Dersom det er rapportert inntekt blir utbetalingsgraden redusert.
 */
public class InnvilgetUttakVurderer implements UttakRegelVurderer {


    private final LocalDateTimeline<Boolean> perioderTilVurdering;
    private final LocalDateTimeline<Boolean> ungdomsprogramtidslinje;
    private final LocalDateTimeline<Set<RapportertInntekt>> rapportertInntektTidslinje;
    private final LocalDateTimeline<UngdomsytelseSatser> satstidslinje;

    public InnvilgetUttakVurderer(LocalDateTimeline<Boolean> perioderTilVurdering,
                                  LocalDateTimeline<Boolean> ungdomsprogramtidslinje,
                                  LocalDateTimeline<Set<RapportertInntekt>> rapportertInntektTidslinje,
                                  LocalDateTimeline<UngdomsytelseSatser> satstidslinje) {
        this.perioderTilVurdering = perioderTilVurdering;
        this.ungdomsprogramtidslinje = ungdomsprogramtidslinje;
        this.rapportertInntektTidslinje = rapportertInntektTidslinje;
        this.satstidslinje = satstidslinje;
    }

    @Override
    public UttakDelResultat vurder() {
        return finnResultatNokDager();
    }

    private UttakDelResultat finnResultatNokDager() {
        final var vurderAntallDagerResultat = FinnForbrukteDager.finnForbrukteDager(ungdomsprogramtidslinje);
        final var tidslinjeNokDagerTilVurdering = vurderAntallDagerResultat.tidslinjeNokDager().intersection(perioderTilVurdering);


        final var uttakPerioder = new ArrayList<UngdomsytelseUttakPeriode>();
        final var redusertUtbetalingsgradResultat = new ReduserVedInntektVurderer(tidslinjeNokDagerTilVurdering, rapportertInntektTidslinje, satstidslinje).vurder();


        uttakPerioder.addAll(redusertUtbetalingsgradResultat.resultatPerioder());
        uttakPerioder.addAll(mapTilUttakPerioderMedNokDagerOgUtbetaling(redusertUtbetalingsgradResultat.restTidslinjeTilVurdering()));
        return new UttakDelResultat(
            uttakPerioder,
            perioderTilVurdering.disjoint(tidslinjeNokDagerTilVurdering),
            Map.of("perioderNokDager", vurderAntallDagerResultat.tidslinjeNokDager().getLocalDateIntervals().toString(),
                "forbrukteDager", String.valueOf(vurderAntallDagerResultat.forbrukteDager()),
                "redusertUtbetalingsgrad", EvaluationPropertiesJsonMapper.mapToJson(redusertUtbetalingsgradResultat.regelSporing()))
        );
    }

    private static List<UngdomsytelseUttakPeriode> mapTilUttakPerioderMedNokDagerOgUtbetaling(LocalDateTimeline<Boolean> tidslinjeNokDagerOgUtbetaling) {
        return tidslinjeNokDagerOgUtbetaling
            .getLocalDateIntervals()
            .stream()
            .map(p -> new UngdomsytelseUttakPeriode(BigDecimal.valueOf(100), DatoIntervallEntitet.fraOgMedTilOgMed(p.getFomDato(), p.getTomDato())))
            .collect(Collectors.toCollection(ArrayList::new));
    }


}
