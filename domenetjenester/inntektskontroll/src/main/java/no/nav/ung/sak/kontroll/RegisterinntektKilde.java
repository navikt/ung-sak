package no.nav.ung.sak.kontroll;

import no.nav.ung.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;

public final class RegisterinntektKilde {

    private RegisterinntektKilde() {
    }

    public static InntektsKilde forYtelse(FagsakYtelseType ytelseType) {
        return switch (ytelseType) {
            case UNGDOMSYTELSE -> InntektsKilde.INNTEKT_UNGDOMSYTELSE;
            case AKTIVITETSPENGER -> InntektsKilde.INNTEKT_AKTIVITETSPENGER;
            default -> throw new IllegalArgumentException("Har ikke inntektskilde for kontroll av registerinntekt for ytelseType=" + ytelseType);
        };
    }
}
