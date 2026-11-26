package no.nav.ung.sak.kontrakt.ekstern.ytelse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.ung.sak.typer.PersonIdent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class YtelseRequestTest {

    private static final PersonIdent IDENT = new PersonIdent("01419010029");
    private static final LocalDate FOM = LocalDate.of(2026, 1, 1);
    private static final LocalDate TOM = LocalDate.of(2026, 12, 31);

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void gyldig_request_skal_ikke_gi_valideringsfeil() {
        assertThat(validator.validate(new YtelseRequest(IDENT, FOM, TOM))).isEmpty();
    }

    // Syntetiske identer fra Dolly: måned + 40 (Nav) eller + 80 (Skatteetaten), d-nummer har i tillegg dag + 40
    @ParameterizedTest
    @ValueSource(strings = {"01419010029", "01819010001", "41419010012", "41819010186"})
    void syntetiske_fnr_og_d_nummer_skal_ikke_gi_valideringsfeil(String ident) {
        assertThat(validator.validate(new YtelseRequest(new PersonIdent(ident), FOM, TOM))).isEmpty();
    }

    @Test
    void fom_lik_tom_er_gyldig() {
        assertThat(validator.validate(new YtelseRequest(IDENT, FOM, FOM))).isEmpty();
    }

    @Test
    void fom_etter_tom_skal_gi_valideringsfeil() {
        var feil = validator.validate(new YtelseRequest(IDENT, TOM, FOM));

        assertThat(feil).extracting(ConstraintViolation::getMessage).containsExactly("fom kan ikke være etter tom");
    }

    @Test
    void ugyldig_ident_skal_gi_valideringsfeil_uten_å_eksponere_ident() {
        var ugyldigIdent = "1234567890";

        var feil = validator.validate(new YtelseRequest(new PersonIdent(ugyldigIdent), FOM, TOM));

        assertThat(feil).singleElement().satisfies(f -> assertThat(f.getMessage()).doesNotContain(ugyldigIdent));
    }

    @Test
    void ident_med_ugyldig_kontrollsiffer_skal_gi_valideringsfeil() {
        var feil = validator.validate(new YtelseRequest(new PersonIdent("01419010028"), FOM, TOM));

        assertThat(feil).extracting(ConstraintViolation::getMessage).containsExactly("ident må være et gyldig fødselsnummer eller d-nummer");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void tom_ident_skal_gi_valideringsfeil_og_ikke_kaste(String tomIdent) {
        var feil = validator.validate(new YtelseRequest(new PersonIdent(tomIdent), FOM, TOM));

        assertThat(feil).extracting(ConstraintViolation::getMessage).containsExactly("ident må være et gyldig fødselsnummer eller d-nummer");
    }

    @ParameterizedTest
    @ValueSource(strings = {"x1419010058", "\u06601419010029"})
    void ident_med_andre_tegn_enn_sifre_skal_gi_valideringsfeil_selv_med_gyldige_kontrollsifre(String ident) {
        var feil = validator.validate(new YtelseRequest(new PersonIdent(ident), FOM, TOM));

        assertThat(feil).extracting(ConstraintViolation::getMessage).containsExactly("ident må være et gyldig fødselsnummer eller d-nummer");
    }

    @Test
    void manglende_felter_skal_gi_valideringsfeil() {
        assertThat(validator.validate(new YtelseRequest(null, null, null))).hasSize(3);
    }

    @Test
    void toString_skal_ikke_inneholde_ident() {
        assertThat(new YtelseRequest(IDENT, FOM, TOM).toString()).doesNotContain(IDENT.getIdent());
    }
}
