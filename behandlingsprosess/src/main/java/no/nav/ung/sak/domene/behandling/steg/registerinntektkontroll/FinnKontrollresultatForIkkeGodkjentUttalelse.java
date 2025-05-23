package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelse.EtterlysningOgRegisterinntekt;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.Set;

public class FinnKontrollresultatForIkkeGodkjentUttalelse {

    static LocalDateTimeline<KontrollResultatType> finnKontrollresultatForIkkeGodkjentUttalelse(
        LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
        LocalDateTimeline<EtterlysningOgRegisterinntekt> relevantIkkeGodkjentUttalelse) {

        final var registerInntektTidslinje = gjeldendeRapporterteInntekter.mapValue(RapporterteInntekter::registerRapporterteInntekter);
        final var ikkeGodkjentUttalelseResultater = relevantIkkeGodkjentUttalelse.combine(registerInntektTidslinje, (di, uttalelse, register) -> {
            if (!harDiff(uttalelse.getValue().registerInntekt(), register != null ? register.getValue() : Set.of())) {
                // Ingen endring i registeropplysninger etter at bruker har gitt uttalelse, oppretter aksjonspunkt
                return new LocalDateSegment<>(di, KontrollResultatType.OPPRETT_AKSJONSPUNKT);
            } else {
                // Nye registeropplysninger etter at bruker har gitt uttalelse, oppretter ny oppgave med ny frist
                return new LocalDateSegment<>(di, KontrollResultatType.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST);
            }
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN);

        return ikkeGodkjentUttalelseResultater;
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

}
