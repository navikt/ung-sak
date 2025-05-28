package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.ytelse.InntektType;
import no.nav.ung.sak.ytelse.RapportertInntekt;
import no.nav.ung.sak.ytelse.RapporterteInntekter;

import java.math.BigDecimal;
import java.util.Set;

public class Avviksvurdering {


    private final BigDecimal akseptertDifferanse;

    public Avviksvurdering(BigDecimal akseptertDifferanse) {
        this.akseptertDifferanse = akseptertDifferanse;
    }

    public LocalDateTimeline<AvvikResultatType> finnAvviksresultatTidslinje(LocalDateTimeline<RapporterteInntekter> gjeldendeRapporterteInntekter,
                                                                            LocalDateTimeline<Boolean> relevantTidslinje) {
        final var brukersRapporteInntekter = gjeldendeRapporterteInntekter
            .intersection(relevantTidslinje)
            .mapValue(RapporterteInntekter::brukerRapporterteInntekter);


        final var registerinntektTidslinje = gjeldendeRapporterteInntekter
            .intersection(relevantTidslinje)
            .mapValue(RapporterteInntekter::registerRapporterteInntekter);


        var diffMotRegister = registerinntektTidslinje.crossJoin(brukersRapporteInntekter,
            (di,
             lhs,
             rhs) -> new LocalDateSegment<>(di, finnSamletKontrollresultat(lhs.getValue(), rhs == null ? Set.of() : rhs.getValue())));
        return diffMotRegister.crossJoin(relevantTidslinje.mapValue(it -> AvvikResultatType.INGEN_AVVIK), StandardCombinators::coalesceLeftHandSide);
    }

    private AvvikResultatType finnSamletKontrollresultat(Set<RapportertInntekt> registerinntekt, Set<RapportertInntekt> rapportertInntekt) {
        final var kontrollResultatATFL = finnKontrollresultatForType(InntektType.ARBEIDSTAKER_ELLER_FRILANSER, registerinntekt, rapportertInntekt);
        final var kontrollResultatYtelse = finnKontrollresultatForType(InntektType.YTELSE, registerinntekt, rapportertInntekt);
        return kontrollResultatATFL.getPrioritet() > kontrollResultatYtelse.getPrioritet() ? kontrollResultatYtelse : kontrollResultatATFL;
    }

    private AvvikResultatType finnKontrollresultatForType(InntektType inntektType, Set<RapportertInntekt> registerinntekter, Set<RapportertInntekt> brukersInntekter) {
        final var register = summer(inntektType, registerinntekter);
        final var bruker = summer(inntektType, brukersInntekter);

        final var differanse = register.subtract(bruker).abs();

        if (differanse.compareTo(akseptertDifferanse) > 0) {
            if (register.compareTo(BigDecimal.ZERO) == 0) {
                return AvvikResultatType.AVVIK_UTEN_REGISTERINNTEKT;
            }
            return AvvikResultatType.AVVIK_MED_REGISTERINNTEKT;
        } else {
            return AvvikResultatType.INGEN_AVVIK;
        }
    }

    private static BigDecimal summer(InntektType inntektType, Set<RapportertInntekt> registerinntekter) {
        return registerinntekter.stream()
            .filter(inntekt -> inntekt.inntektType().equals(inntektType))
            .map(RapportertInntekt::beløp).reduce(BigDecimal::add).orElse(BigDecimal.ZERO);
    }


}
