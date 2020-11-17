package no.nav.k9.sak.ytelse.unntaksbehandling.beregning;

import static no.nav.k9.sak.ytelse.unntaksbehandling.beregning.Datoer.dato;

import java.time.LocalDate;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import no.nav.k9.sak.kontrakt.beregningsresultat.TilkjentYtelsePeriodeDto;
import no.nav.vedtak.exception.FunksjonellException;

class TilkjentYtelsePerioderValidatorTest {

    @Test
    void overlappende_perioder_skal_gi_feilmelding() {
        // Arrange
        TilkjentYtelsePeriodeDto p1 = enPeriode(dato("2020.01.10"), dato("2020.01.20"));
        TilkjentYtelsePeriodeDto p2 = enPeriode(dato("2020.01.15"), dato("2020.01.25"));

        // Act og Assert
        Assertions.assertThatCode(
            () -> TilkjentYtelsePerioderValidator.valider(List.of(p1, p2))
        )
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Det er angitt overlappende perioder med tilkjent ytelse")
            .hasMessageContaining("2020-01-15, 2020-01-25")
            .hasMessageContaining("2020-01-10, 2020-01-20")
            .hasFieldOrPropertyWithValue("kode", "K9-123456");
    }

    @Test
    void duplikate_perioder_skal_gi_feilmelding() {
        // Arrange
        TilkjentYtelsePeriodeDto p1 = enPeriode(dato("2020.01.10"), dato("2020.01.20"));
        TilkjentYtelsePeriodeDto p2 = enPeriode(dato("2020.01.10"), dato("2020.01.20"));

        // Act og Assert
        Assertions.assertThatCode(
            () -> TilkjentYtelsePerioderValidator.valider(List.of(p1, p2))
        )
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Det er angitt overlappende perioder med tilkjent ytelse")
            .hasMessageContaining("2020-01-10, 2020-01-20")
            .hasFieldOrPropertyWithValue("kode", "K9-123456");
    }
    @Test
    void perioder_uten_overlapp_skal_gå_ok() {
        // Arrange
        TilkjentYtelsePeriodeDto p1 = enPeriode(dato("2020.01.10"), dato("2020.01.20"));
        TilkjentYtelsePeriodeDto p2 = enPeriode(dato("2020.01.21"), dato("2020.01.25"));

        // Act og Assert
        Assertions.assertThatCode(
            () -> TilkjentYtelsePerioderValidator.valider(List.of(p1, p2))
        )
            .doesNotThrowAnyException();
    }

    private TilkjentYtelsePeriodeDto enPeriode(LocalDate fom, LocalDate tom) {
        return TilkjentYtelsePeriodeDto.build(fom, tom).create();
    }
}
