package no.nav.ung.sak.domene.behandling.steg.beregning;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.grunnbeløp.GrunnbeløpTjeneste;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.BarnetilleggVurdering;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.LagBarnetilleggTidslinje;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngdomsytelseBeregnDagsatsTest {

    private UngdomsytelseBeregnDagsats tjeneste;
    @Inject
    private GrunnbeløpTjeneste grunnbeløpTjeneste;
    private LagBarnetilleggTidslinje lagBarnetilleggTidslinje = mock(LagBarnetilleggTidslinje.class);


    @BeforeEach
    void setUp() {
        tjeneste = new UngdomsytelseBeregnDagsats(new LagGrunnbeløpTidslinjeTjeneste(grunnbeløpTjeneste), lagBarnetilleggTidslinje);
        when(lagBarnetilleggTidslinje.lagTidslinje(any(), any())).thenReturn(new BarnetilleggVurdering(LocalDateTimeline.empty(), List.of()));
    }


    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_april_2024_og_bruker_18_år_ved_start() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 4, 15);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18).minusDays(1);
        var dagsatsTidslinje = tjeneste.beregnDagsats(null, perioder, fødselsdag, LocalDate.now(), false);

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(1);

        var first = segmenter.first();
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(tom);
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.33333));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(608.31));
    }


    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_18_år_måneden_før_start() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 15);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18).minusDays(1);
        var dagsatsTidslinje = tjeneste.beregnDagsats(null, perioder, fødselsdag, LocalDate.now(), false);

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(2);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(LocalDate.of(2024, 4, 30));
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.33333));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(608.31));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(LocalDate.of(2024, 5, 1));
        assertThat(second.getTom()).isEqualTo(tom);
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.33333));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(636.04));
    }

    @Test
    void skal_beregne_lav_dagsats_for_hele_perioden_med_start_i_mars_2024_og_slutt_i_mai_2024_selv_om_bruker_blir_25_år_midt_i_april_når_det_beregnes_før_bruker_er_25_år() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 4, 15);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = tjeneste.beregnDagsats(null, perioder, fødselsdato, tjuefemårsdag.minusDays(1), false);

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(2);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        var førsteDagMedHøySats = LocalDate.of(2024, 5, 1);
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(førsteDagMedHøySats.minusDays(1));
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.33333));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(608.31));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(førsteDagMedHøySats);
        assertThat(second.getTom()).isEqualTo(tom);
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.33333));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(636.04));

    }

    @Test
    void skal_beregne_lav_og_høy_dagsats_for_en_perioder_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_midt_i_april_når_det_beregnes_når_bruker_har_blitt_25_år() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var datoForGRegulering = LocalDate.of(2024, 5, 1);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 4, 15);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = tjeneste.beregnDagsats(null, perioder, fødselsdato, tjuefemårsdag.plusDays(1), false);

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(3);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        var førsteDagMedHøySats = tjuefemårsdag;
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(førsteDagMedHøySats.minusDays(1));
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.33333));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(608.31));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(tjuefemårsdag);
        assertThat(second.getTom()).isEqualTo(datoForGRegulering.minusDays(1));
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(912.46));

        var third = iterator.next();
        assertThat(third.getFom()).isEqualTo(datoForGRegulering);
        assertThat(third.getTom()).isEqualTo(tom);
        assertThat(third.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(third.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(third.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(954.06));
    }

    @Test
    void skal_beregne_lav_og_høy_dagsats_for_en_perioder_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_midt_i_april_før_bruker_er_25_år_når_det_finnes_trigger_for_beregningen() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var datoForGRegulering = LocalDate.of(2024, 5, 1);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 4, 15);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = tjeneste.beregnDagsats(null, perioder, fødselsdato, tjuefemårsdag.minusDays(1), true);

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(3);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        var førsteDagMedHøySats = LocalDate.of(2024, 4, 14);
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(førsteDagMedHøySats);
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.33333));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(608.31));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(førsteDagMedHøySats.plusDays(1));
        assertThat(second.getTom()).isEqualTo(tom.minusMonths(1));
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(912.46));

        // Etter G-regulering
        var third = iterator.next();
        assertThat(third.getFom()).isEqualTo(datoForGRegulering);
        assertThat(third.getTom()).isEqualTo(tom);
        assertThat(third.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(third.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(third.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(954.06));
    }


    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_første_april() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var datoForGRegulering = LocalDate.of(2024, 5, 1);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 4, 1);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = tjeneste.beregnDagsats(null, perioder, fødselsdato, LocalDate.now(), true);

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(3);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        assertThat(first.getFom()).isEqualTo(fom);
        var sisteDagMedLavSats = tjuefemårsdag.minusDays(1);
        assertThat(first.getTom()).isEqualTo(sisteDagMedLavSats);
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.33333));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(608.31));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(tjuefemårsdag);
        assertThat(second.getTom()).isEqualTo(datoForGRegulering.minusDays(1));
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(912.46));

        var third = iterator.next();
        assertThat(third.getFom()).isEqualTo(datoForGRegulering);
        assertThat(third.getTom()).isEqualTo(tom);
        assertThat(third.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(third.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(third.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(954.06));
    }


    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_på_dato_g_regulering() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 5, 1);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = tjeneste.beregnDagsats(null, perioder, fødselsdato, LocalDate.now(), true);

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(2);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        assertThat(first.getFom()).isEqualTo(fom);
        var sisteDagMedLavSats = tjuefemårsdag.minusDays(1);
        assertThat(first.getTom()).isEqualTo(sisteDagMedLavSats);
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.33333));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(608.31));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(tjuefemårsdag);
        assertThat(second.getTom()).isEqualTo(tom);
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(954.06));
    }
}
