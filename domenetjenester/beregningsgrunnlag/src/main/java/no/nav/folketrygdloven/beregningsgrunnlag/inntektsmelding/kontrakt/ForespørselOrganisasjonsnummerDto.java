package no.nav.folketrygdloven.beregningsgrunnlag.inntektsmelding.kontrakt;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ForespørselOrganisasjonsnummerDto(@JsonValue @NotNull @Pattern(regexp = VALID_REGEXP, message = "orgnr ${validatedValue} har ikke gyldig verdi (pattern '{regexp}')") String orgnr) {
    private static final String VALID_REGEXP = "^\\d{9}$";

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        var that = (ForespørselOrganisasjonsnummerDto) obj;
        return Objects.equals(this.orgnr, that.orgnr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orgnr);
    }

    @Override
    public String toString() {
        return "ForespørselOrganisasjonsnummerDto[" + "orgnr=" + orgnr.substring(0, Math.min(orgnr.length(), 3)) + "...]"; // Skjuler fullt orgnr (sensitiv informasjon i kontekst av ytelse)
    }
}
