package no.nav.ung.sak.formidling.innhold;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BarnetilleggEndringsdatoUtlederTest {

    private static final LocalDate STARTDATO = LocalDate.of(2026, 10, 1);
    private static final LocalDate SLUTTDATO = LocalDate.of(2027, 9, 29);
    private static final LocalDateTimeline<Boolean> SATS_TIDSLINJE = new LocalDateTimeline<>(STARTDATO, SLUTTDATO, true);

    @Test
    void barn_født_før_startdato_gir_startdato() {
        var fødselTidslinje = new LocalDateTimeline<>(STARTDATO.minusDays(21), SLUTTDATO, true);

        var dato = BarnetilleggEndringsdatoUtleder.finnFørsteDatoMedSats(fødselTidslinje, SATS_TIDSLINJE);

        assertThat(dato).isEqualTo(STARTDATO);
    }

    @Test
    void barn_født_på_startdato_gir_startdato() {
        var fødselTidslinje = new LocalDateTimeline<>(STARTDATO, SLUTTDATO, true);

        var dato = BarnetilleggEndringsdatoUtleder.finnFørsteDatoMedSats(fødselTidslinje, SATS_TIDSLINJE);

        assertThat(dato).isEqualTo(STARTDATO);
    }

    @Test
    void barn_født_etter_startdato_gir_fødselsdato() {
        var fødselsdato = STARTDATO.plusDays(10);
        var fødselTidslinje = new LocalDateTimeline<>(fødselsdato, SLUTTDATO, true);

        var dato = BarnetilleggEndringsdatoUtleder.finnFørsteDatoMedSats(fødselTidslinje, SATS_TIDSLINJE);

        assertThat(dato).isEqualTo(fødselsdato);
    }

    @Test
    void feiler_når_fødselsperioden_ikke_har_sats() {
        var fødselTidslinje = new LocalDateTimeline<>(STARTDATO.minusDays(21), STARTDATO.minusDays(1), true);

        assertThatThrownBy(() -> BarnetilleggEndringsdatoUtleder.finnFørsteDatoMedSats(fødselTidslinje, SATS_TIDSLINJE))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Fant ingen sats");
    }
}
