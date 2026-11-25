package no.nav.ung.sak.kontrakt.ekstern.ytelse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class YtelseResponseTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void skal_validere_vedtak_og_perioder_i_listene() {
        var periodeUtenTom = new YtelsePeriode(LocalDate.of(2026, 1, 1), null, null, null, null, null);
        var vedtakUtenYtelse = new Ytelse(null, "SAK1", UUID.randomUUID(), LocalDateTime.now(), List.of(periodeUtenTom));

        var feil = validator.validate(new YtelseResponse(List.of(vedtakUtenYtelse)));

        assertThat(feil).extracting(f -> f.getPropertyPath().toString())
            .containsExactlyInAnyOrder("vedtak[0].ytelse", "vedtak[0].perioder[0].tom");
    }

    @Test
    void gyldig_svar_skal_ikke_gi_valideringsfeil() {
        var periode = new YtelsePeriode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, null, 0, 0);
        var vedtak = new Ytelse(YtelseType.AKTIVITETSPENGER, "SAK1", UUID.randomUUID(), LocalDateTime.now(), List.of(periode));

        assertThat(validator.validate(new YtelseResponse(List.of(vedtak)))).extracting(ConstraintViolation::getMessage).isEmpty();
    }
}
