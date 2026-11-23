package no.nav.ung.sak.kontrakt.hendelser;

import java.io.IOException;
import java.time.LocalDateTime;

import jakarta.validation.Validation;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.ung.kontrakt.JsonUtil;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HendelseInfoTest {

    private static final LocalDateTime OPPRETTET = LocalDateTime.of(2026, 11, 23, 7, 0);

    @Test
    void aktørider_er_kaskadevalidert_på_container_elementet() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var property = factory.getValidator()
                .getConstraintsForClass(HendelseInfo.class)
                .getConstraintsForProperty("aktørIder");

            assertThat(property.isCascaded()).isFalse();
            assertThat(property.getConstrainedContainerElementTypes())
                .singleElement()
                .satisfies(containerElement -> assertThat(containerElement.isCascaded()).isTrue());
        }
    }

    @Test
    void gyldig_syntetisk_aktørid_er_gyldig() {
        var hendelseInfo = new HendelseInfo.Builder()
            .leggTilAktør(new AktørId("123"))
            .medOpprettet(OPPRETTET)
            .build();

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(hendelseInfo)).isEmpty();
        }
    }

    @Test
    void ugyldig_aktørid_gir_pattern_violation_med_forventet_sti() {
        var hendelseInfo = new HendelseInfo.Builder()
            .leggTilAktør(new AktørId("abc"))
            .medOpprettet(OPPRETTET)
            .build();

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(hendelseInfo))
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getConstraintDescriptor().getAnnotation()).isInstanceOf(Pattern.class);
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("aktørIder[0].aktørId");
                });
        }
    }

    @Test
    void mer_enn_hundre_aktørider_gir_size_violation() {
        var builder = new HendelseInfo.Builder().medOpprettet(OPPRETTET);
        for (int i = 0; i < 101; i++) {
            builder.leggTilAktør(new AktørId("123"));
        }
        var hendelseInfo = builder.build();

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(hendelseInfo))
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getConstraintDescriptor().getAnnotation()).isInstanceOf(Size.class);
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("aktørIder");
                });
        }
    }

    @Test
    void null_liste_gir_not_null_violation() throws IOException {
        var hendelseInfo = JsonUtil.fromJson("""
            {"aktørIder": null, "opprettet": "2026-11-23T07:00:00"}
            """, HendelseInfo.class);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validate(hendelseInfo))
                .singleElement()
                .satisfies(violation -> {
                    assertThat(violation.getConstraintDescriptor().getAnnotation()).isInstanceOf(NotNull.class);
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("aktørIder");
                });
        }
    }
}
