package no.nav.k9.sak.domene.typer.tid;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;

class IntervallUtilTest {

    LocalDate mandag = LocalDate.of(2023, 2, 20);
    LocalDate søndag = LocalDate.of(2023, 2, 26);

    @Test
    void skal_telle_alle_dager_med_beregnKalenderdager() {
        assertThat(IntervallUtil.beregnKalanderdager(new LocalDateInterval(mandag, mandag))).isEqualTo(1);
        assertThat(IntervallUtil.beregnKalanderdager(new LocalDateInterval(søndag, søndag))).isEqualTo(1);
        assertThat(IntervallUtil.beregnKalanderdager(new LocalDateInterval(mandag, søndag))).isEqualTo(7);
    }

    @Test
    void skal_telle_ikke_telle_helg_i_beregnUkedager() {
        assertThat(IntervallUtil.beregnUkedager(mandag, mandag)).isEqualTo(1);
        assertThat(IntervallUtil.beregnUkedager(søndag, søndag)).isEqualTo(0);
        assertThat(IntervallUtil.beregnUkedager(mandag, søndag)).isEqualTo(5);
    }
}
