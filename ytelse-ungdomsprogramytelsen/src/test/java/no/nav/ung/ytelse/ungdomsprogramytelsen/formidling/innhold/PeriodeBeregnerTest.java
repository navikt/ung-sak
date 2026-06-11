package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.innhold;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodeBeregnerTest {

    @Test
    void utleder_siste_utbetalingsdato_til_12_i_neste_måned() {
        var sluttdato = LocalDate.of(2026, 6, 30);

        var dato = PeriodeBeregner.utledFremtidigUtbetalingsdato(sluttdato, YearMonth.of(2026, 6));

        assertThat(dato).isEqualTo(LocalDate.of(2026, 7, 12));
    }

    @Test
    void utleder_ingen_fremtidig_utbetalingsdato_for_tidligere_måneder() {
        var sluttdato = LocalDate.of(2026, 5, 31);

        var dato = PeriodeBeregner.utledFremtidigUtbetalingsdato(sluttdato, YearMonth.of(2026, 6));

        assertThat(dato).isNull();
    }
}

