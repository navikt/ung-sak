package no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.Set;

import static no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.FinnResultatForEndretRegisteropplysninger.Endringsresultat.ENDRING;
import static no.nav.ung.sak.domene.behandling.steg.kompletthet.registerinntektkontroll.FinnResultatForEndretRegisteropplysninger.Endringsresultat.INGEN_ENDRING;

public class FinnResultatForEndretRegisteropplysninger {

    static LocalDateTimeline<Endringsresultat> finnTidslinjeForEndring(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<EtterlysningOgRegisterinntekt> etterlysningOgRegisterinntekt) {

        final var registerInntektTidslinje = gjeldendeRapporterteInntekter.mapValue(RapporterteInntekter::registerRapporterteInntekter);

        return etterlysningOgRegisterinntekt.combine(registerInntektTidslinje, (di, uttalelse, register) -> {
            if (harDiff(uttalelse.getValue().registerInntekt(), register != null ? register.getValue() : Set.of())) {
                // Nye registeropplysninger
                return new LocalDateSegment<>(di, ENDRING);
            } else {
                // Ingen nye registeropplysninger
                return new LocalDateSegment<>(di, INGEN_ENDRING);
            }
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN);
    }

    static boolean harDiff(Set<RapportertInntekt> registerInntekFraUttalelse, Set<RapportertInntekt> gjeldendeRegisterinntekt) {
        final var totalInntektFraUttalelse = summerInntekter(registerInntekFraUttalelse);
        final var totalGjeldendeRegisterInntekt = summerInntekter(gjeldendeRegisterinntekt);
        return totalInntektFraUttalelse.compareTo(totalGjeldendeRegisterInntekt) != 0;
    }

    private static BigDecimal summerInntekter(Set<RapportertInntekt> registerinntektFraUttalelse) {
        return registerinntektFraUttalelse.stream()
            .map(RapportertInntekt::beløp)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    }

    public enum Endringsresultat {
        INGEN_ENDRING,
        ENDRING
    }

}
