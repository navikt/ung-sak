package no.nav.k9.sak.ytelse.ung.beregning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.KalkulusTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Grunnbeløp;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class UngdomsytelseBeregnDagsatsTest {

    private UngdomsytelseBeregnDagsats tjeneste;
    private KalkulusTjeneste kalkulusTjeneste = mock(KalkulusTjeneste.class);

    @BeforeEach
    void setUp() {
        tjeneste = new UngdomsytelseBeregnDagsats(new LagGrunnbeløpTidslinjeTjeneste(kalkulusTjeneste));
        when(kalkulusTjeneste.hentGrunnbeløp(LocalDate.of(2023,5,1))).thenReturn(new Grunnbeløp(100000, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2023,5,1), LocalDate.of(2024,4,30))));
        when(kalkulusTjeneste.hentGrunnbeløp(LocalDate.of(2024,5,1))).thenReturn(new Grunnbeløp(124028, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2024,5,1), LocalDate.MAX)));
    }

    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_april_2024_og_bruker_18_år_ved_start() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 4, 15);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18).minusDays(1);
        var dagsatsTidslinje = tjeneste.beregnDagsats(perioder, fødselsdag);

        var segmenter = dagsatsTidslinje.toSegments();
        assertThat(segmenter.size()).isEqualTo(1);

        var first = segmenter.first();
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(tom);
        assertThat(first.getValue().grunnbeløpFaktor().compareTo(BigDecimal.valueOf(1.33333))).isEqualTo(0);
        assertThat(first.getValue().grunnbeløp().compareTo(BigDecimal.valueOf(100000))).isEqualTo(0);
        assertThat(first.getValue().dagsats().compareTo(BigDecimal.valueOf(512.82))).isEqualTo(0);
    }


    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_18_år_måneden_før_start() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 15);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18).minusDays(1);
        var dagsatsTidslinje = tjeneste.beregnDagsats(perioder, fødselsdag);

        var segmenter = dagsatsTidslinje.toSegments();
        assertThat(segmenter.size()).isEqualTo(2);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(LocalDate.of(2024,4, 30));
        assertThat(first.getValue().grunnbeløpFaktor().compareTo(BigDecimal.valueOf(1.33333))).isEqualTo(0);
        assertThat(first.getValue().grunnbeløp().compareTo(BigDecimal.valueOf(100000))).isEqualTo(0);
        assertThat(first.getValue().dagsats().compareTo(BigDecimal.valueOf(512.82))).isEqualTo(0);

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(LocalDate.of(2024,5, 1));
        assertThat(second.getTom()).isEqualTo(tom);
        assertThat(second.getValue().grunnbeløpFaktor().compareTo(BigDecimal.valueOf(1.33333))).isEqualTo(0);
        assertThat(second.getValue().grunnbeløp().compareTo(BigDecimal.valueOf(124028))).isEqualTo(0);
        assertThat(second.getValue().dagsats().compareTo(BigDecimal.valueOf(636.04))).isEqualTo(0);
    }

    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_midt_i_april() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 4, 15);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = tjeneste.beregnDagsats(perioder, fødselsdato);

        var segmenter = dagsatsTidslinje.toSegments();
        assertThat(segmenter.size()).isEqualTo(2);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        var førsteDagMedHøySats = LocalDate.of(2024, 5, 1);
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(førsteDagMedHøySats.minusDays(1));
        assertThat(first.getValue().grunnbeløpFaktor().compareTo(BigDecimal.valueOf(1.33333))).isEqualTo(0);
        assertThat(first.getValue().grunnbeløp().compareTo(BigDecimal.valueOf(100000))).isEqualTo(0);
        assertThat(first.getValue().dagsats().compareTo(BigDecimal.valueOf(512.82))).isEqualTo(0);

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(førsteDagMedHøySats);
        assertThat(second.getTom()).isEqualTo(tom);
        assertThat(second.getValue().grunnbeløpFaktor().compareTo(BigDecimal.valueOf(2))).isEqualTo(0);
        assertThat(second.getValue().grunnbeløp().compareTo(BigDecimal.valueOf(124028))).isEqualTo(0);
        assertThat(second.getValue().dagsats().compareTo(BigDecimal.valueOf(954.06))).isEqualTo(0);
    }


    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_første_april() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 4, 1);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = tjeneste.beregnDagsats(perioder, fødselsdato);

        var segmenter = dagsatsTidslinje.toSegments();
        assertThat(segmenter.size()).isEqualTo(2);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        assertThat(first.getFom()).isEqualTo(fom);
        var sisteDagMedLavSats = tjuefemårsdag.with(TemporalAdjusters.lastDayOfMonth());
        assertThat(first.getTom()).isEqualTo(sisteDagMedLavSats);
        assertThat(first.getValue().grunnbeløpFaktor().compareTo(BigDecimal.valueOf(1.33333))).isEqualTo(0);
        assertThat(first.getValue().grunnbeløp().compareTo(BigDecimal.valueOf(100000))).isEqualTo(0);
        assertThat(first.getValue().dagsats().compareTo(BigDecimal.valueOf(512.82))).isEqualTo(0);

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(sisteDagMedLavSats.plusDays(1));
        assertThat(second.getTom()).isEqualTo(tom);
        assertThat(second.getValue().grunnbeløpFaktor().compareTo(BigDecimal.valueOf(2))).isEqualTo(0);
        assertThat(second.getValue().grunnbeløp().compareTo(BigDecimal.valueOf(124028))).isEqualTo(0);
        assertThat(second.getValue().dagsats().compareTo(BigDecimal.valueOf(954.06))).isEqualTo(0);
    }



}
