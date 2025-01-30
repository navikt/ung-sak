package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.*;

import java.time.LocalDate;
import java.util.*;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_BEGYNNELSE;
import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;

class VurderUttakTjeneste {


    static Optional<UngdomsytelseUttakPerioder> vurderUttak(LocalDateTimeline<Boolean> godkjentePerioder,
                                                            LocalDateTimeline<Boolean> ungdomsprogramtidslinje,
                                                            Optional<LocalDate> søkersDødsdato,
                                                            LocalDateTimeline<UngdomsytelseSatser> satsTidslinje,
                                                            LocalDateTimeline<Set<RapportertInntekt>> rapporterteInntekterTidslinje) {
        if (godkjentePerioder.isEmpty()) {
            return Optional.empty();
        }

        var levendeBrukerTidslinje = søkersDødsdato.map(d -> new LocalDateTimeline<>(TIDENES_BEGYNNELSE, d, true)).orElse(new LocalDateTimeline<>(TIDENES_BEGYNNELSE, TIDENES_ENDE, true));


        // Finner tidslinje med nok dager tilgjengelig
        List<UttakDelResultat> delresultater = new ArrayList<>();
        final var nokDagerDelresultat = new InnvilgetUttakVurderer(
            godkjentePerioder,
            ungdomsprogramtidslinje,
            levendeBrukerTidslinje,
            rapporterteInntekterTidslinje,
            satsTidslinje).vurder();
        delresultater.add(nokDagerDelresultat);

        final var uttakAvslagEtterSøkersDødDelResultat = new AvslagVedDødVurderer(nokDagerDelresultat.restTidslinjeTilVurdering(), levendeBrukerTidslinje).vurder();
        delresultater.add(uttakAvslagEtterSøkersDødDelResultat);

        final var ikkeNokDagerPeriodeDelResultat = new AvslagIkkeNokDagerVurderer(uttakAvslagEtterSøkersDødDelResultat.restTidslinjeTilVurdering()).vurder();
        delresultater.add(ikkeNokDagerPeriodeDelResultat);

        final var uttakPerioder = delresultater.stream().map(UttakDelResultat::resultatPerioder)
                .flatMap(List::stream)
                .toList();

        var ungdomsytelseUttakPerioder = new UngdomsytelseUttakPerioder(uttakPerioder);
        ungdomsytelseUttakPerioder.setRegelInput(lagRegelInput(godkjentePerioder, ungdomsprogramtidslinje, søkersDødsdato));
        ungdomsytelseUttakPerioder.setRegelSporing(lagSporing(delresultater));
        return Optional.of(ungdomsytelseUttakPerioder);
    }

    private static String lagSporing(List<UttakDelResultat> delresultater) {
        return delresultater.stream().map(UttakDelResultat::regelSporing)
                .reduce((m1, m2) -> {
                    final var newMap = new HashMap<>(m2);
                    newMap.putAll(m1);
                    return newMap;
                })
                .map(EvaluationPropertiesJsonMapper::mapToJson)
                .orElse("");
    }

    private static String lagRegelInput(LocalDateTimeline<Boolean> godkjentePerioder, LocalDateTimeline<Boolean> ungdomsprogramtidslinje, Optional<LocalDate> søkersDødsdato) {
        return """
                {
                    "godkjentePerioder": ":godkjentePerioder",
                    "ungdomsprogramtidslinje": ":ungdomsprogramtidslinje",
                    "søkersDødsdato": :søkersDødsdato
                }
                """.stripLeading()
                .replaceFirst(":godkjentePerioder", godkjentePerioder.getLocalDateIntervals().toString())
                .replaceFirst(":ungdomsprogramtidslinje", ungdomsprogramtidslinje.getLocalDateIntervals().toString())
                .replaceFirst(":søkersDødsdato", søkersDødsdato.map(Objects::toString).map(it -> "\"" + it + "\"").orElse("null"));
    }



}
