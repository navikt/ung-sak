package no.nav.ung.sak.kontroll;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;

/**
 * Utleder inntekt som skal brukes til reduksjon av ytelsen.
 * <p>
 * Registerinntekt følger kontantprinsippet, og summen for en måned kan bli negativ, f.eks. når arbeidsgiver korrigerer
 * tidligere rapportert inntekt. Negativ inntekt skal ikke øke ytelsen, så summen per inntektstype settes til minst 0 kr
 * før den brukes som kontrollert inntekt. Negativ inntekt for én type motregnes ikke mot en annen type.
 */
public final class InntektMedNedreGrense {

    private InntektMedNedreGrense() {
    }

    public static BigDecimal summerForType(InntektType inntektType, Set<RapportertInntekt> inntekter) {
        return inntekter.stream()
            .filter(it -> it.inntektType().equals(inntektType))
            .map(RapportertInntekt::beløp)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO)
            .max(BigDecimal.ZERO);
    }

    public static BigDecimal summerAlleTyper(Set<RapportertInntekt> inntekter) {
        return Arrays.stream(InntektType.values())
            .map(type -> summerForType(type, inntekter))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
