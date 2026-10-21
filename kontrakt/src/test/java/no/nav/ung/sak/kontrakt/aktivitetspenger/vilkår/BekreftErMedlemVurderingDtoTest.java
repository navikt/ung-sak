package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.medlemskap.MedlemskapAvslagsÅrsakType;
import no.nav.ung.sak.typer.Periode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BekreftErMedlemVurderingDtoTest {

    private static final List<Periode> PERIODER = List.of(new Periode(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void innvilget_uten_fritekst_er_gyldig() {
        var dto = new BekreftErMedlemVurderingDto("begrunnelse", true, null, PERIODER, null);

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void avslag_med_fritekst_er_gyldig() {
        var dto = new BekreftErMedlemVurderingDto("begrunnelse", false, MedlemskapAvslagsÅrsakType.SØKER_IKKE_MEDLEM, PERIODER, "Du har ikke vært medlem i folketrygden.");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void avslag_uten_fritekst_gir_valideringsfeil() {
        var dto = new BekreftErMedlemVurderingDto("begrunnelse", false, MedlemskapAvslagsÅrsakType.SØKER_IKKE_MEDLEM, PERIODER, null);

        assertThat(validator.validate(dto)).extracting(ConstraintViolation::getMessage)
            .containsExactly("fritekstVurderingBrev må være satt hvis erVilkårInnvilget er false");
    }

    @Test
    void avslag_med_blank_fritekst_gir_valideringsfeil() {
        var dto = new BekreftErMedlemVurderingDto("begrunnelse", false, MedlemskapAvslagsÅrsakType.SØKER_IKKE_MEDLEM, PERIODER, "   ");

        assertThat(validator.validate(dto)).extracting(ConstraintViolation::getMessage)
            .contains("fritekstVurderingBrev må være satt hvis erVilkårInnvilget er false");
    }
}
