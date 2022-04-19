package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;

class NyoppstartetUtlederTjenesteTest {

    LocalDate dag0 = LocalDate.now();

    @Test
    void skal_si_at_nyoppstartet_første_4_uker_når_aldri_har_jobbet_hos_arbeidsgiver() {
        var aktivtArbeidsforhold = new LocalDateTimeline<>(dag0, dag0.plusDays(1000), true);
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(dag0, dag0.plusWeeks(4).minusDays(1), true)); //minus 1 dag siden slutt av perioden er inkludert
    }

    @Test
    void skal_si_at_nyoppstartet_første_4_uker_etter_opphold_på_over_2_uker() {
        var aktivtArbeidsforhold = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag0.plusWeeks(10).minusDays(1), true),
            new LocalDateSegment<>(dag0.plusWeeks(12), dag0.plusWeeks(100), true)));
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag0.plusWeeks(4).minusDays(1), true),
            new LocalDateSegment<>(dag0.plusWeeks(12), dag0.plusWeeks(12 + 4).minusDays(1), true))));
    }

    @Test
    void skal_si_at_nyoppstartet_første_4_uker_etter_opphold_på_over_2_uker_kort_arbeidsperiode() {
        var aktivtArbeidsforhold = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag0, true),
            new LocalDateSegment<>(dag0.plusWeeks(3), dag0.plusWeeks(3), true)));
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag0, true),
            new LocalDateSegment<>(dag0.plusWeeks(3), dag0.plusWeeks(3), true))));
    }
}
