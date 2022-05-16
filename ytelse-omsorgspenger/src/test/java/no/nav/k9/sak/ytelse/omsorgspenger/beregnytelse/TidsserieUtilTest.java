package no.nav.k9.sak.ytelse.omsorgspenger.beregnytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.TidslinjeUtil;

class TidsserieUtilTest {

    @Test
    void kunPerioderSomIkkeFinnesISkalStøtteEnkeltdag() {
        var perioder = new LocalDateTimeline<>(LocalDate.of(2020, 1, 5), LocalDate.of(2020, 1, 5), true);
        var perdioderSomTrekkesFra = new LocalDateTimeline<>(LocalDate.of(2020, 1, 5), LocalDate.of(2020, 1, 5), true);
        var utvidedePerioder = TidslinjeUtil.kunPerioderSomIkkeFinnesI(perioder, perdioderSomTrekkesFra);
        var segments = utvidedePerioder.toSegments();
        assertThat(segments).isEmpty();
    }

    @Test
    void kunPerioderSomIkkeFinnesISkalStøtteEnkeltdagMidten() {
        var perioder = new LocalDateTimeline<>(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5), true);
        var perdioderSomTrekkesFra = new LocalDateTimeline<>(LocalDate.of(2020, 1, 3), LocalDate.of(2020, 1, 3), true);
        var utvidedePerioder = TidslinjeUtil.kunPerioderSomIkkeFinnesI(perioder, perdioderSomTrekkesFra);
        var segments = utvidedePerioder.toSegments();
        assertThat(segments).hasSize(2);
        assertThat(segments.first().getFom()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(segments.first().getTom()).isEqualTo(LocalDate.of(2020, 1, 2));
        assertThat(segments.last().getFom()).isEqualTo(LocalDate.of(2020, 1, 4));
        assertThat(segments.last().getTom()).isEqualTo(LocalDate.of(2020, 1, 5));

    }

    @Test
    void kunPerioderSomIkkeFinnesISkalStøtteEnkeltdagSlutten() {
        var perioder = new LocalDateTimeline<>(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 5), true);
        var perdioderSomTrekkesFra = new LocalDateTimeline<>(LocalDate.of(2020, 1, 5), LocalDate.of(2020, 1, 5), true);
        var utvidedePerioder = TidslinjeUtil.kunPerioderSomIkkeFinnesI(perioder, perdioderSomTrekkesFra);
        var segments = utvidedePerioder.toSegments();
        assertThat(segments).hasSize(1);
        assertThat(segments.first().getFom()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(segments.first().getTom()).isEqualTo(LocalDate.of(2020, 1, 4));
    }

}
