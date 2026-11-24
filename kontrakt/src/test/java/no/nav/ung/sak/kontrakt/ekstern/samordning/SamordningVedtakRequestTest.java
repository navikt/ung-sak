package no.nav.ung.sak.kontrakt.ekstern.samordning;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SamordningVedtakRequestTest {

    private static final PersonIdent IDENT = new PersonIdent("01419010029");
    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 12, 31);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void gyldig_request_skal_ikke_gi_valideringsfeil() {
        assertThat(validator.validate(new SamordningVedtakRequest(IDENT, FOM, TOM))).isEmpty();
    }

    @Test
    void fom_lik_tom_er_gyldig() {
        assertThat(validator.validate(new SamordningVedtakRequest(IDENT, FOM, FOM))).isEmpty();
    }

    @Test
    void fom_etter_tom_skal_gi_valideringsfeil() {
        var feil = validator.validate(new SamordningVedtakRequest(IDENT, TOM, FOM));

        assertThat(feil).extracting(ConstraintViolation::getMessage).containsExactly("fom kan ikke være etter tom");
    }

    @Test
    void ugyldig_ident_skal_gi_valideringsfeil_uten_å_eksponere_ident() {
        var ugyldigIdent = "1234567890";

        var feil = validator.validate(new SamordningVedtakRequest(new PersonIdent(ugyldigIdent), FOM, TOM));

        assertThat(feil).singleElement().satisfies(f -> assertThat(f.getMessage()).doesNotContain(ugyldigIdent));
    }

    @Test
    void ident_med_ugyldig_kontrollsiffer_skal_gi_valideringsfeil() {
        var feil = validator.validate(new SamordningVedtakRequest(new PersonIdent("01419010028"), FOM, TOM));

        assertThat(feil).extracting(ConstraintViolation::getMessage).containsExactly("ident må være et gyldig fødselsnummer eller d-nummer");
    }

    @Test
    void manglende_felter_skal_gi_valideringsfeil() {
        assertThat(validator.validate(new SamordningVedtakRequest(null, null, null))).hasSize(3);
    }

    @Test
    void toString_skal_ikke_inneholde_ident() {
        assertThat(new SamordningVedtakRequest(IDENT, FOM, TOM).toString()).doesNotContain(IDENT.getIdent());
    }
}
