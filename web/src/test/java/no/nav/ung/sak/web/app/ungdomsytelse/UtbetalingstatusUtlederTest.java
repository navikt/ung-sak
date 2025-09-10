package no.nav.ung.sak.web.app.ungdomsytelse;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.kontrakt.ungdomsytelse.ytelse.UtbetalingStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class UtbetalingstatusUtlederTest {

    @Test
    void skal_håndtere_tom_tilkjent_ytelse() {
        var aktuellBehandlingAvsluttetTidspunkt = new BehandlingAvsluttetTidspunkt(LocalDateTime.now());
        var dagensDato = LocalDate.now();
        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            aktuellBehandlingAvsluttetTidspunkt,
            Map.of(aktuellBehandlingAvsluttetTidspunkt, LocalDateTimeline.empty()), dagensDato
        );

        assertThat(utbetalingstatusTidslinje.isEmpty()).isTrue();
    }

    @Test
    void skal_utlede_status_for_førstegangsbehandling_med_utbetaling_frem_i_tid() {
        var aktuellBehandlingAvsluttetTidspunkt = new BehandlingAvsluttetTidspunkt(LocalDateTime.now());
        var ytelseFom = aktuellBehandlingAvsluttetTidspunkt.avsluttetTid().toLocalDate().plusMonths(1).withDayOfMonth(1);
        var ytelseTom = aktuellBehandlingAvsluttetTidspunkt.avsluttetTid().toLocalDate().plusMonths(1).withDayOfMonth(15);

        var tilkjentYtelseTidslinje = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100, BigDecimal.TEN )
        );
        var dagensDato = LocalDate.now();
        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            aktuellBehandlingAvsluttetTidspunkt,
            Map.of(aktuellBehandlingAvsluttetTidspunkt, tilkjentYtelseTidslinje), dagensDato
        );

        // Assert
        var forventet = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            UtbetalingStatus.TIL_UTBETALING
        );
        assertThat(utbetalingstatusTidslinje.isEmpty()).isFalse();
        assertThat(utbetalingstatusTidslinje).isEqualTo(forventet);
    }

    @Test
    void skal_utlede_status_for_førstegangsbehandling_med_utbetaling_tilbake_i_tid_med_passert_utbetalingsdag() {
        var dagensDato = LocalDate.of(2025, 6, 5);
        var aktuellBehandlingAvsluttetTidspunkt = new BehandlingAvsluttetTidspunkt(dagensDato.minusDays(10).atStartOfDay());
        var ytelseFom = LocalDate.of(2025, 5, 1);
        var ytelseTom = LocalDate.of(2025, 5, 15);

        var tilkjentYtelseTidslinje = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100, BigDecimal.TEN  )
        );
        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            aktuellBehandlingAvsluttetTidspunkt,
            Map.of(aktuellBehandlingAvsluttetTidspunkt, tilkjentYtelseTidslinje), dagensDato
        );

        // Assert
        var forventet = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            UtbetalingStatus.UTBETALT
        );
        assertThat(utbetalingstatusTidslinje.isEmpty()).isFalse();
        assertThat(utbetalingstatusTidslinje).isEqualTo(forventet);
    }

    @Test
    void skal_utlede_status_for_førstegangsbehandling_med_utbetaling_tilbake_i_tid_med_passert_utbetalingsdag_uten_avsluttet_behandling() {
        var dagensDato = LocalDate.of(2025, 6, 5);
        var aktuellBehandlingAvsluttetTidspunkt = new BehandlingAvsluttetTidspunkt(null);
        var ytelseFom = LocalDate.of(2025, 5, 1);
        var ytelseTom = LocalDate.of(2025, 5, 15);

        var tilkjentYtelseTidslinje = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN  )
        );
        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            aktuellBehandlingAvsluttetTidspunkt,
            Map.of(aktuellBehandlingAvsluttetTidspunkt, tilkjentYtelseTidslinje), dagensDato
        );

        // Assert
        var forventet = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            UtbetalingStatus.TIL_UTBETALING
        );
        assertThat(utbetalingstatusTidslinje.isEmpty()).isFalse();
        assertThat(utbetalingstatusTidslinje).isEqualTo(forventet);
    }


    @Test
    void skal_utlede_status_for_førstegangsbehandling_med_utbetaling_tilbake_i_tid_med_avsluttet_behandling_etter_første_virkedag() {
        var dagensDato = LocalDate.of(2025, 6, 5);
        var førsteVirkedag = LocalDate.of(2025, 6, 2);
        var aktuellBehandlingAvsluttetTidspunkt = new BehandlingAvsluttetTidspunkt(førsteVirkedag.plusDays(1).atStartOfDay());
        var ytelseFom = LocalDate.of(2025, 5, 1);
        var ytelseTom = LocalDate.of(2025, 5, 15);

        var tilkjentYtelseTidslinje = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN  )
        );
        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            aktuellBehandlingAvsluttetTidspunkt,
            Map.of(aktuellBehandlingAvsluttetTidspunkt, tilkjentYtelseTidslinje), dagensDato
        );

        // Assert
        var forventet = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            UtbetalingStatus.TIL_UTBETALING
        );
        assertThat(utbetalingstatusTidslinje.isEmpty()).isFalse();
        assertThat(utbetalingstatusTidslinje).isEqualTo(forventet);
    }

    @Test
    void skal_utlede_status_for_førstegangsbehandling_med_utbetaling_tilbake_i_tid_med_avsluttet_behandling_etter_første_virkedag_med_dagens_dato_lik_utbetalingsdato() {
        var dagensDato = LocalDate.of(2025, 6, 6);
        var førsteVirkedag = LocalDate.of(2025, 6, 2);
        var aktuellBehandlingAvsluttetTidspunkt = new BehandlingAvsluttetTidspunkt(førsteVirkedag.plusDays(1).atStartOfDay());
        var ytelseFom = LocalDate.of(2025, 5, 1);
        var ytelseTom = LocalDate.of(2025, 5, 15);

        var tilkjentYtelseTidslinje = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN  )
        );
        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            aktuellBehandlingAvsluttetTidspunkt,
            Map.of(aktuellBehandlingAvsluttetTidspunkt, tilkjentYtelseTidslinje), dagensDato
        );

        // Assert
        var forventet = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            UtbetalingStatus.UTBETALT
        );
        assertThat(utbetalingstatusTidslinje.isEmpty()).isFalse();
        assertThat(utbetalingstatusTidslinje).isEqualTo(forventet);
    }



    @Test
    void skal_utlede_status_for_revurdering_med_ingen_endring() {
        var dagensDato = LocalDate.of(2025, 6, 10);
        var førstegangsbehandlingAvsluttetTid = new BehandlingAvsluttetTidspunkt(dagensDato.minusDays(10).atStartOfDay());
        var andregangsbehandlingAvsluttetTid = new BehandlingAvsluttetTidspunkt(dagensDato.atStartOfDay());

        var ytelseFom = LocalDate.of(2025, 5, 1);
        var ytelseTom = LocalDate.of(2025, 5, 15);

        var tilkjentYtelseTidslinje = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN  )
        );
        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            andregangsbehandlingAvsluttetTid,
            Map.of(førstegangsbehandlingAvsluttetTid, tilkjentYtelseTidslinje,
                andregangsbehandlingAvsluttetTid, tilkjentYtelseTidslinje), dagensDato
        );

        // Assert
        var forventet = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            UtbetalingStatus.UTBETALT
        );
        assertThat(utbetalingstatusTidslinje.isEmpty()).isFalse();
        assertThat(utbetalingstatusTidslinje).isEqualTo(forventet);
    }

    @Test
    void skal_utlede_status_for_revurdering_med_endring() {
        var dagensDato = LocalDate.of(2025, 6, 10);
        var førstegangsbehandlingAvsluttetTid = new BehandlingAvsluttetTidspunkt(dagensDato.minusDays(10).atStartOfDay());
        var revurderingAvsluttetTid = new BehandlingAvsluttetTidspunkt(dagensDato.atStartOfDay());

        var ytelseFom = LocalDate.of(2025, 5, 1);
        var ytelseTom = LocalDate.of(2025, 5, 15);

        var tilkjentYtelseTidslinje = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN  )
        );

        var tilkjentYtelseTidslinjeRevurdering = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN  )
        );
        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            revurderingAvsluttetTid,
            Map.of(førstegangsbehandlingAvsluttetTid, tilkjentYtelseTidslinje,
                revurderingAvsluttetTid, tilkjentYtelseTidslinjeRevurdering), dagensDato
        );

        // Assert
        var forventet = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            UtbetalingStatus.TIL_UTBETALING
        );
        assertThat(utbetalingstatusTidslinje.isEmpty()).isFalse();
        assertThat(utbetalingstatusTidslinje).isEqualTo(forventet);
    }

    @Test
    void skal_utlede_status_for_revurdering_med_endring_uten_avsluttet_revurdering() {
        var dagensDato = LocalDate.of(2025, 6, 10);
        var førstegangsbehandlingAvsluttetTid = new BehandlingAvsluttetTidspunkt(dagensDato.minusDays(10).atStartOfDay());
        var revurderingAvsluttetTid = new BehandlingAvsluttetTidspunkt(null);

        var ytelseFom = LocalDate.of(2025, 5, 1);
        var ytelseTom = LocalDate.of(2025, 5, 15);

        var tilkjentYtelseTidslinje = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN  )
        );

        var tilkjentYtelseTidslinjeRevurdering = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            new TilkjentYtelseVerdi(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN  )
        );
        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            revurderingAvsluttetTid,
            Map.of(førstegangsbehandlingAvsluttetTid, tilkjentYtelseTidslinje,
                revurderingAvsluttetTid, tilkjentYtelseTidslinjeRevurdering), dagensDato
        );

        // Assert
        var forventet = new LocalDateTimeline<>(ytelseFom, ytelseTom,
            UtbetalingStatus.TIL_UTBETALING
        );
        assertThat(utbetalingstatusTidslinje.isEmpty()).isFalse();
        assertThat(utbetalingstatusTidslinje).isEqualTo(forventet);
    }


    @Test
    void skal_utlede_status_for_revurdering_med_ny_periode_uten_avsluttet_revurdering() {
        var dagensDato = LocalDate.of(2025, 6, 10);
        var førstegangsbehandlingAvsluttetTid = new BehandlingAvsluttetTidspunkt(dagensDato.minusDays(10).atStartOfDay());
        var revurderingAvsluttetTid = new BehandlingAvsluttetTidspunkt(null);

        var periode1Fom = LocalDate.of(2025, 4, 1);
        var periode1Tom = LocalDate.of(2025, 4, 30);
        var periode2Fom = LocalDate.of(2025, 5, 1);
        var periode2Tom = LocalDate.of(2025, 5, 31);

        var tilkjentYtelseTidslinje = new LocalDateTimeline<>(periode1Fom, periode1Tom,
            new TilkjentYtelseVerdi(BigDecimal.TEN, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN  )
        );

        var tilkjentYtelseTidslinjeRevurdering = new LocalDateTimeline<>(periode2Fom, periode2Tom,
            new TilkjentYtelseVerdi(BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.TEN, BigDecimal.TEN, 100,BigDecimal.TEN )
        ).crossJoin(tilkjentYtelseTidslinje);


        var utbetalingstatusTidslinje = UtbetalingstatusUtleder.finnUtbetalingsstatusTidslinje(
            revurderingAvsluttetTid,
            Map.of(førstegangsbehandlingAvsluttetTid, tilkjentYtelseTidslinje,
                revurderingAvsluttetTid, tilkjentYtelseTidslinjeRevurdering), dagensDato
        );

        // Assert
        var forventet = new LocalDateTimeline<>(periode1Fom, periode1Tom,
            UtbetalingStatus.UTBETALT
        ).crossJoin(new LocalDateTimeline<>(periode2Fom, periode2Tom,
            UtbetalingStatus.TIL_UTBETALING
        ));
        assertThat(utbetalingstatusTidslinje.isEmpty()).isFalse();
        assertThat(utbetalingstatusTidslinje).isEqualTo(forventet);
    }






}
