package no.nav.k9.sak.typer;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

import org.junit.Test;

public class PeriodeAndelTest {

    @Test
    public void skal_konvertere_BigDecimal_timer_til_Duration() throws Exception {
        var fom = LocalDate.now();
        var tom = fom.plusDays(10);
        var p = new Periode(fom, tom);
        assertThat(new PeriodeAndel(p, new BigDecimal("7.5")).getVarighetPerDag()).isEqualTo(Duration.parse("PT7H30M"));
        assertThat(new PeriodeAndel(p, new BigDecimal("0.5")).getVarighetPerDag()).isEqualTo(Duration.parse("PT30M"));
        assertThat(new PeriodeAndel(p, new BigDecimal("6.3")).getVarighetPerDag()).isEqualTo(Duration.parse("PT6H30M"));
        assertThat(new PeriodeAndel(p, new BigDecimal("6.33")).getVarighetPerDag()).isEqualTo(Duration.parse("PT6H30M"));
        assertThat(new PeriodeAndel(p, new BigDecimal("6.24")).getVarighetPerDag()).isEqualTo(Duration.parse("PT6H"));
        assertThat(new PeriodeAndel(p, new BigDecimal("6.25")).getVarighetPerDag()).isEqualTo(Duration.parse("PT6H30M"));
        assertThat(new PeriodeAndel(p, new BigDecimal("6.75")).getVarighetPerDag()).isEqualTo(Duration.parse("PT7H"));
    }
}
