package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;
import no.nav.ung.sak.ytelse.uttalelse.BrukersUttalelseForRegisterinntekt;

import java.math.BigDecimal;
import java.util.Set;

public class FinnKontrollresultatForIkkeGodkjentUttalelse {

    static KontrollResultat finnKontrollresultatForIkkeGodkjentUttalelse(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter, LocalDateTimeline<BrukersUttalelseForRegisterinntekt> relevantIkkeGodkjentUttalelse) {
        final var registerInntektTidslinje = gjeldendeRapporterteInntekter.mapValue(RapporterteInntekter::getRegisterRapporterteInntekter);
        final var ikkeGodkjentUttalelseResultater = relevantIkkeGodkjentUttalelse.combine(registerInntektTidslinje, (di, uttalelse, register) -> {
            if (!harDiff(uttalelse.getValue().registerInntekt(), register != null ? register.getValue() : Set.of())) {
                return new LocalDateSegment<>(di, KontrollResultat.OPPRETT_AKSJONSPUNKT);
            } else {
                return new LocalDateSegment<>(di, KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST);
            }
        }, LocalDateTimeline.JoinStyle.LEFT_JOIN);

        if (!ikkeGodkjentUttalelseResultater.filterValue(it -> it == KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST).isEmpty()) {
            return KontrollResultat.OPPRETT_OPPGAVE_TIL_BRUKER_MED_NY_FRIST;
        } else if (!ikkeGodkjentUttalelseResultater.filterValue(it -> it == KontrollResultat.OPPRETT_AKSJONSPUNKT).isEmpty()) {
            return KontrollResultat.OPPRETT_AKSJONSPUNKT;
        }

        return KontrollResultat.OPPRETT_AKSJONSPUNKT;
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
