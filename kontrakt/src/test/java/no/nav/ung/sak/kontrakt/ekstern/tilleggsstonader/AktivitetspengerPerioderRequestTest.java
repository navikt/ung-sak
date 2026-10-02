package no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AktivitetspengerPerioderRequestTest {

    private static final String IDENT = "12345678901";
    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 12, 31);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void gyldig_request_skal_ikke_gi_valideringsfeil() {
        assertThat(validator.validate(new AktivitetspengerPerioderRequest(IDENT, FOM, TOM))).isEmpty();
    }

    @Test
    void fom_lik_tom_er_gyldig() {
        assertThat(validator.validate(new AktivitetspengerPerioderRequest(IDENT, FOM, FOM))).isEmpty();
    }

    @Test
    void fom_etter_tom_skal_gi_valideringsfeil() {
        var feil = validator.validate(new AktivitetspengerPerioderRequest(IDENT, TOM, FOM));

        assertThat(feil).extracting(ConstraintViolation::getMessage).containsExactly("fom kan ikke være etter tom");
    }

    @Test
    void ugyldig_ident_skal_gi_valideringsfeil_uten_å_eksponere_ident() {
        var ugyldigIdent = "1234567890";

        Set<ConstraintViolation<AktivitetspengerPerioderRequest>> feil = validator.validate(new AktivitetspengerPerioderRequest(ugyldigIdent, FOM, TOM));

        assertThat(feil).hasSize(1);
        assertThat(feil.iterator().next().getMessage()).doesNotContain(ugyldigIdent);
    }

    @Test
    void manglende_felter_skal_gi_valideringsfeil() {
        assertThat(validator.validate(new AktivitetspengerPerioderRequest(null, null, null))).hasSize(3);
    }

    @Test
    void toString_skal_ikke_inneholde_ident() {
        assertThat(new AktivitetspengerPerioderRequest(IDENT, FOM, TOM).toString()).doesNotContain(IDENT);
    }
}
