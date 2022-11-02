package no.nav.k9.sak.kontrakt.beregninginput;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import no.nav.k9.sak.typer.OrgNummer;

class OverstyrBeregningAktivitetTest {


    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new Jdk8Module())
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.NONE)
        .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        .setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();


    @Test
    void skal_returnere_false_ved_manglende_orgnr_og_aktørid() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(null, null, 1, 1, null, null, true);
        var violations = validerAnnotasjoner(overstyrBeregningAktivitet);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(overstyrBeregningAktivitet.isOrgnrEllerAktørid()).isFalse();
    }

    @Test
    void skal_returnere_true_ved_orgnr() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, null, null, true);
        var violations = validerAnnotasjoner(overstyrBeregningAktivitet);
        assertThat(violations.size()).isEqualTo(0);
        assertThat(overstyrBeregningAktivitet.isOrgnrEllerAktørid()).isTrue();
    }

    @Test
    void skal_returnere_false_ved_startdato_lik_opphør() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, LocalDate.now(), LocalDate.now(), true);
        var violations = validerAnnotasjoner(overstyrBeregningAktivitet);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(overstyrBeregningAktivitet.isStartdatoRefusjonFørOpphør()).isFalse();
    }

    @Test
    void skal_returnere_false_ved_startdato_etter_opphør() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, LocalDate.now().plusDays(1), LocalDate.now(), true);
        var violations = validerAnnotasjoner(overstyrBeregningAktivitet);
        assertThat(violations.size()).isEqualTo(1);
        assertThat(overstyrBeregningAktivitet.isStartdatoRefusjonFørOpphør()).isFalse();
    }

    @Test
    void skal_returnere_true_ved_startdato_før_opphør() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, LocalDate.now().minusDays(1), LocalDate.now(), true);
        var violations = validerAnnotasjoner(overstyrBeregningAktivitet);
        assertThat(violations.size()).isEqualTo(0);
        assertThat(overstyrBeregningAktivitet.isStartdatoRefusjonFørOpphør()).isTrue();
    }

    @Test
    void skal_returnere_true_når_startdato_er_null() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, null, LocalDate.now(), true);
        var violations = validerAnnotasjoner(overstyrBeregningAktivitet);
        assertThat(violations.size()).isEqualTo(0);
        assertThat(overstyrBeregningAktivitet.isStartdatoRefusjonFørOpphør()).isTrue();
    }

    @Test
    void skal_returnere_true_når_opphør_er_null() {
        var overstyrBeregningAktivitet = new OverstyrBeregningAktivitet(new OrgNummer("910909088"), null, 1, 1, LocalDate.now(), null, true);
        var violations = validerAnnotasjoner(overstyrBeregningAktivitet);
        assertThat(violations.size()).isEqualTo(0);
        assertThat(overstyrBeregningAktivitet.isStartdatoRefusjonFørOpphør()).isTrue();
    }

    private Set<ConstraintViolation<OverstyrBeregningAktivitet>> validerAnnotasjoner(OverstyrBeregningAktivitet overstyrBeregningAktivitet) {
        var jsonValue = toJson(overstyrBeregningAktivitet);
        var deserialised = fromJson(jsonValue);
        var violations = VALIDATOR.validate(deserialised);
        return violations;
    }

    public static String toJson(OverstyrBeregningAktivitet object) {
        try {
            Writer jsonWriter = new StringWriter();
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonWriter, object);
            jsonWriter.flush();
            return jsonWriter.toString();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static OverstyrBeregningAktivitet fromJson(String json) {
        try {
            return objectMapper.readerFor(OverstyrBeregningAktivitet.class).readValue(json);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
