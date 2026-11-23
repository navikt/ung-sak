package no.nav.ung.sak.kontrakt.produksjonsstyring;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SøkeSakEllerBrukerDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void saksnummer_som_søkestreng_er_gyldig() {
        assertThat(validator.validate(new SøkeSakEllerBrukerDto("AB12C"))).isEmpty();
    }

    @Test
    void tom_søkestreng_skal_gi_valideringsfeil() {
        var feil = validator.validate(new SøkeSakEllerBrukerDto(""));

        assertThat(feil).extracting(v -> v.getPropertyPath().toString()).containsExactly("searchString");
    }

}
