package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static java.util.List.of;
import static no.nav.k9.sak.ytelse.unntaksbehandling.beregning.Datoer.dato;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.vedtak.exception.FunksjonellException;

class TilkjentYtelsePerioderValidatorTest {

    @Test
    void overlappende_perioder_skal_gi_feilmelding() {
        // Arrange
        TilkjentYtelsePeriodeDto p1 = enTilkjentYtelsePeriode(dato("2020.01.10"), dato("2020.01.20"));
        TilkjentYtelsePeriodeDto p2 = enTilkjentYtelsePeriode(dato("2020.01.15"), dato("2020.01.25"));

        // Act og Assert
        Assertions.assertThatCode(
            () -> TilkjentYtelsePerioderValidator.validerOmOverlappendePerioder(of(p1, p2))
        )
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Det er angitt overlappende perioder med tilkjent ytelse")
            .hasMessageContaining("2020-01-15, 2020-01-25")
            .hasMessageContaining("2020-01-10, 2020-01-20")
            .hasFieldOrPropertyWithValue("kode", "K9-951877");
    }

    @Test
    void duplikate_perioder_skal_gi_feilmelding() {
        // Arrange
        TilkjentYtelsePeriodeDto p1 = enTilkjentYtelsePeriode(dato("2020.01.10"), dato("2020.01.20"));
        TilkjentYtelsePeriodeDto p2 = enTilkjentYtelsePeriode(dato("2020.01.10"), dato("2020.01.20"));

        // Act og Assert
        Assertions.assertThatCode(
            () -> TilkjentYtelsePerioderValidator.validerOmOverlappendePerioder(of(p1, p2))
        )
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Det er angitt overlappende perioder med tilkjent ytelse")
            .hasMessageContaining("2020-01-10, 2020-01-20")
            .hasFieldOrPropertyWithValue("kode", "K9-951877");
    }

    @Test
    void perioder_uten_overlapp_skal_gå_ok() {
        // Arrange
        TilkjentYtelsePeriodeDto p1 = enTilkjentYtelsePeriode(dato("2020.01.10"), dato("2020.01.20"));
        TilkjentYtelsePeriodeDto p2 = enTilkjentYtelsePeriode(dato("2020.01.21"), dato("2020.01.25"));

        // Act og Assert
        Assertions.assertThatCode(
            () -> TilkjentYtelsePerioderValidator.validerOmOverlappendePerioder(of(p1, p2))
        )
            .doesNotThrowAnyException();
    }

    @Test
    void tilkjent_ytelse_innenfor_vilkåret_er_ok() {
        // Arrange Act Assert
        assertValiderVilkårsperiode(
            of(
                enTilkjentYtelsePeriode(dato("2020.01.10"), dato("2020.01.20")),
                enTilkjentYtelsePeriode(dato("2020.01.21"), dato("2020.01.25"))
            ),
            etVilkår(dato("2020.01.10"), dato("2020.01.25"))
        )
            .doesNotThrowAnyException();
    }

    @Test
    void tilkjent_ytelse_er_utenfor_vilkåret_skal_gi_feilmelding() {
        // Arrange Act Assert
        assertValiderVilkårsperiode(
            of(
                enTilkjentYtelsePeriode(dato("2020.01.10"), dato("2020.01.14")),
                enTilkjentYtelsePeriode(dato("2020.01.15"), dato("2020.01.20"))
            ),
            etVilkår(dato("2020.01.11"), dato("2020.01.19"))
        )
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Periode med tilkjent ytelse er ikke innenfor vilkåret")
            .hasFieldOrPropertyWithValue("kode", "K9-951878");
    }

    @Test
    void tilkjent_ytelse_fom_er_utenfor_vilkåret_skal_gi_feilmelding() {
        // Arrange Act Assert
        assertValiderVilkårsperiode(
            of(
                enTilkjentYtelsePeriode(dato("2020.01.10"), dato("2020.01.14")),
                enTilkjentYtelsePeriode(dato("2020.01.15"), dato("2020.01.20"))
            ),
            etVilkår(dato("2020.01.11"), dato("2020.01.20"))
        )
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Periode med tilkjent ytelse er ikke innenfor vilkåret")
            .hasFieldOrPropertyWithValue("kode", "K9-951878");
    }

    @Test
    void tilkjent_ytelse_tom_er_utenfor_vilkåret_skal_gi_feilmelding() {
        // Arrange Act Assert
        assertValiderVilkårsperiode(
            of(
                enTilkjentYtelsePeriode(dato("2020.01.10"), dato("2020.01.14")),
                enTilkjentYtelsePeriode(dato("2020.01.15"), dato("2020.01.20"))
            ),
            etVilkår(dato("2020.01.10"), dato("2020.01.19"))
        )
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Periode med tilkjent ytelse er ikke innenfor vilkåret")
            .hasFieldOrPropertyWithValue("kode", "K9-951878");
    }

    @NotNull
    private AbstractThrowableAssert<?, ? extends Throwable> assertValiderVilkårsperiode(List<TilkjentYtelsePeriodeDto> ytelser, Vilkår vilkår) {
        return Assertions.assertThatCode(
            () -> TilkjentYtelsePerioderValidator.validerVilkårsperiode(ytelser, vilkår)
        );
    }

    private Vilkår etVilkår(LocalDate fom, LocalDate tom) {
        return new VilkårBuilder(VilkårType.K9_VILKÅRET)
            .leggTil(
                new VilkårPeriodeBuilder()
                    .medPeriode(fom, tom)
            )
            .build();
    }

    private TilkjentYtelsePeriodeDto enTilkjentYtelsePeriode(LocalDate fom, LocalDate tom) {
        return TilkjentYtelsePeriodeDto.build(fom, tom).create();
    }
}
