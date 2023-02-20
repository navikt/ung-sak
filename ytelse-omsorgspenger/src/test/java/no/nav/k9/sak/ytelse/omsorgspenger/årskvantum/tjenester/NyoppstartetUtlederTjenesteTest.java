package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.fpsak.tidsserie.StandardCombinators;

class NyoppstartetUtlederTjenesteTest {

    LocalDate dag0 = LocalDate.now();

    @Test
    void skal_si_at_nyoppstartet_første_4_uker_når_aldri_har_jobbet_hos_arbeidsgiver() {
        var aktivtArbeidsforhold = new LocalDateTimeline<>(dag0, dag0.plusDays(1000), NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV);
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(dag0, dag0.plusWeeks(4).minusDays(1), true)); //minus 1 dag siden slutt av perioden er inkludert
    }

    @Test
    void skal_si_at_nyoppstartet_første_4_uker_etter_opphold_på_over_2_uker() {
        var aktivtArbeidsforhold = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag0.plusWeeks(10).minusDays(1), NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV),
            new LocalDateSegment<>(dag0.plusWeeks(12), dag0.plusWeeks(100), NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV)));
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag0.plusWeeks(4).minusDays(1), true),
            new LocalDateSegment<>(dag0.plusWeeks(12), dag0.plusWeeks(12 + 4).minusDays(1), true))));
    }

    @Test
    void skal_si_at_nyoppstartet_første_4_uker_etter_opphold_på_over_2_uker_kort_arbeidsperiode() {
        var aktivtArbeidsforhold = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag0, NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV),
            new LocalDateSegment<>(dag0.plusWeeks(8), dag0.plusWeeks(8), NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV)));
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag0.plusWeeks(4).minusDays(1), true),
            new LocalDateSegment<>(dag0.plusWeeks(8), dag0.plusWeeks(12).minusDays(1), true))));
    }

    @Test
    void skal_si_at_nyoppstartet_første_4_uker_også_i_kort_permisjonssperiode() {
        LocalDate dag1 = dag0.plusDays(1);
        var aktivtArbeidsforhold = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag0.plusDays(1000), NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV),
            new LocalDateSegment<>(dag1, dag1, NyoppstartetUtleder.ArbeidsforholdStatus.PERMISJON)),
            StandardCombinators::coalesceRightHandSide
        );
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(dag0, dag0.plusWeeks(4).minusDays(1), true)); //minus 1 dag siden slutt av perioden er inkludert
    }

    @Test
    void skal_si_at_nyoppstartet_første_4_uker_også_når_arbeidsforholdet_starter_med_permisjon_på_under_2_uker() {
        LocalDate dag1 = dag0.plusWeeks(2).minusDays(1);
        var aktivtArbeidsforhold = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag1.minusDays(1), NyoppstartetUtleder.ArbeidsforholdStatus.PERMISJON),
            new LocalDateSegment<>(dag1, dag1.plusDays(1000), NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV)
        ));
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(dag0, dag0.plusWeeks(4).minusDays(1), true)); //minus 1 dag siden slutt av perioden er inkludert
    }

    @Test
    void skal_si_at_nyoppstartet_etter_permisjonsslutt_når_arbeidsforholdet_starter_med_permisjon_på_2_uker() {
        LocalDate dag1 = dag0.plusWeeks(2);
        var aktivtArbeidsforhold = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag1.minusDays(1), NyoppstartetUtleder.ArbeidsforholdStatus.PERMISJON),
            new LocalDateSegment<>(dag1, dag1.plusDays(1000), NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV)
        ));
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(dag1, dag1.plusWeeks(4).minusDays(1), true)); //minus 1 dag siden slutt av perioden er inkludert
    }

    @Test
    void skal_si_at_nyoppstartet_første_4_uker_også_når_man_får_lang_perimisjon_fra_midt_i_perioden() {
        LocalDate dag1 = dag0.plusWeeks(2);
        var aktivtArbeidsforhold = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag1.minusDays(1), NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV),
            new LocalDateSegment<>(dag1, dag1.plusDays(1000), NyoppstartetUtleder.ArbeidsforholdStatus.PERMISJON)
        ));
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(dag0, dag0.plusWeeks(4).minusDays(1), true)); //minus 1 dag siden slutt av perioden er inkludert
    }

    @Test
    void skal_si_at_nyoppstartet_fra_permisjonsslutt_når_man_får_lang_perimisjon_i_starten_av_arbeidsforholdet() {
        LocalDate dag1 = dag0.plusWeeks(2);
        var aktivtArbeidsforhold = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(dag0, dag1.minusDays(1), NyoppstartetUtleder.ArbeidsforholdStatus.PERMISJON),
            new LocalDateSegment<>(dag1, dag1.plusDays(1000), NyoppstartetUtleder.ArbeidsforholdStatus.AKTIV)
        ));
        var nyoppstartetPerioder = NyoppstartetUtleder.nyoppstartetPerioder(aktivtArbeidsforhold);
        assertThat(nyoppstartetPerioder).isEqualTo(new LocalDateTimeline<>(dag1, dag1.plusWeeks(4).minusDays(1), true)); //minus 1 dag siden slutt av perioden er inkludert
    }
}
