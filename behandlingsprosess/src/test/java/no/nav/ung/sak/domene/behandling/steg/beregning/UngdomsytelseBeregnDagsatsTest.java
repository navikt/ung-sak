package no.nav.ung.sak.domene.behandling.steg.beregning;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.BeregnDagsatsInput;
import no.nav.ung.sak.domene.behandling.steg.beregning.barnetillegg.FødselOgDødInfo;
import no.nav.ung.sak.typer.AktørId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngdomsytelseBeregnDagsatsTest {


    public static final BigDecimal DAGSATS_LAV_SATS = BigDecimal.valueOf(649.0798666826);
    public static final BigDecimal GRUNNBELØP = BigDecimal.valueOf(124028);
    public static final BigDecimal GRUNNBELØP_FAKTOR_LAV_SATS = BigDecimal.valueOf(1.3606666667);
    public static final int DAGSATS_BARNETILLEGG = 37;

    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_april_2024_og_bruker_18_år_ved_start() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 4, 15);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18).minusDays(1);
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(
            lagInput(perioder, fødselsdag));

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(1);

        var first = segmenter.first();
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(tom);
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.3606666667));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(620.7780000152));
    }


    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_18_år_måneden_før_start() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 15);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18).minusDays(1);
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(lagInput(perioder, fødselsdag));

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(2);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(LocalDate.of(2024, 4, 30));
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.3606666667));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(620.7780000152));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(LocalDate.of(2024, 5, 1));
        assertThat(second.getTom()).isEqualTo(tom);
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.3606666667));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(649.0798666826));
    }

    @Test
    void skal_beregne_lav_dagsats_for_hele_perioden_med_start_i_mars_2024_og_slutt_i_mai_2024_selv_om_bruker_blir_25_år_midt_i_april_når_det_beregnes_før_bruker_er_25_år() {
        var fom = LocalDate.now().plusMonths(1).withDayOfMonth(1);
        var tom = LocalDate.now().plusMonths(3).withDayOfMonth(1).minusDays(1);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = fom.plusMonths(1).plusDays(14);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(lagInput(perioder, fødselsdato));

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(1);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(tjuefemårsdag.minusDays(1));
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.36067));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(130160));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(681.17));
    }

    @Test
    void skal_beregne_lav_og_høy_dagsats_for_en_perioder_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_midt_i_april_når_det_beregnes_når_bruker_har_fått_høy_sats_tidligere() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var datoForGRegulering = LocalDate.of(2024, 5, 1);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 4, 15);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(new BeregnDagsatsInput(perioder, fødselsdato, false, true, List.of()));

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(3);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        var førsteDagMedHøySats = tjuefemårsdag;
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(førsteDagMedHøySats.minusDays(1));
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.3606666667));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(620.7780000152));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(tjuefemårsdag);
        assertThat(second.getTom()).isEqualTo(datoForGRegulering.minusDays(1));
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2.041));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(931.1670000000));

        var third = iterator.next();
        assertThat(third.getFom()).isEqualTo(datoForGRegulering);
        assertThat(third.getTom()).isEqualTo(tom);
        assertThat(third.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2.041));
        assertThat(third.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(third.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(973.6198000000));
    }

    @Test
    void skal_beregne_lav_og_høy_dagsats_for_en_perioder_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_midt_i_april_før_bruker_er_25_år_når_det_finnes_trigger_for_beregningen() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var datoForGRegulering = LocalDate.of(2024, 5, 1);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 4, 15);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(
            lagInput(perioder, fødselsdato, true));

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(3);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        var førsteDagMedHøySats = LocalDate.of(2024, 4, 14);
        assertThat(first.getFom()).isEqualTo(fom);
        assertThat(first.getTom()).isEqualTo(førsteDagMedHøySats);
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.3606666667));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(620.7780000152));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(førsteDagMedHøySats.plusDays(1));
        assertThat(second.getTom()).isEqualTo(tom.minusMonths(1));
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2.041));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(931.1670000000));

        // Etter G-regulering
        var third = iterator.next();
        assertThat(third.getFom()).isEqualTo(datoForGRegulering);
        assertThat(third.getTom()).isEqualTo(tom);
        assertThat(third.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2.041));
        assertThat(third.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(third.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(973.6198000000));
    }


    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_første_april() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var datoForGRegulering = LocalDate.of(2024, 5, 1);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 4, 1);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(lagInput(perioder, fødselsdato, true));

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(3);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        assertThat(first.getFom()).isEqualTo(fom);
        var sisteDagMedLavSats = tjuefemårsdag.minusDays(1);
        assertThat(first.getTom()).isEqualTo(sisteDagMedLavSats);
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.3606666667));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(620.7780000152));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(tjuefemårsdag);
        assertThat(second.getTom()).isEqualTo(datoForGRegulering.minusDays(1));
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2.041));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(931.1670000000));

        var third = iterator.next();
        assertThat(third.getFom()).isEqualTo(datoForGRegulering);
        assertThat(third.getTom()).isEqualTo(tom);
        assertThat(third.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2.041));
        assertThat(third.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(third.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(973.6198000000));
    }


    @Test
    void skal_beregne_dagsats_for_en_periode_med_start_i_mars_2024_og_slutt_i_mai_2024_og_bruker_blir_25_år_på_dato_g_regulering() {
        var fom = LocalDate.of(2024, 3, 1);
        var tom = LocalDate.of(2024, 5, 30);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var tjuefemårsdag = LocalDate.of(2024, 5, 1);
        var fødselsdato = tjuefemårsdag.minusYears(25);
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(lagInput(perioder, fødselsdato, true));

        var segmenter = dagsatsTidslinje.resultatTidslinje().toSegments();
        assertThat(segmenter.size()).isEqualTo(2);

        var iterator = segmenter.iterator();
        var first = iterator.next();
        assertThat(first.getFom()).isEqualTo(fom);
        var sisteDagMedLavSats = tjuefemårsdag.minusDays(1);
        assertThat(first.getTom()).isEqualTo(sisteDagMedLavSats);
        assertThat(first.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(1.3606666667));
        assertThat(first.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(118620));
        assertThat(first.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(620.7780000152));

        var second = iterator.next();
        assertThat(second.getFom()).isEqualTo(tjuefemårsdag);
        assertThat(second.getTom()).isEqualTo(tom);
        assertThat(second.getValue().grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2.041));
        assertThat(second.getValue().grunnbeløp()).isEqualByComparingTo(BigDecimal.valueOf(124028));
        assertThat(second.getValue().dagsats()).isEqualByComparingTo(BigDecimal.valueOf(973.6198000000));
    }

    @Test
    void skal_beregne_dagsats_med_ett_barn_død_i_hele_perioden() {
        var fom = LocalDate.of(2025, 1, 1);
        var tom = LocalDate.of(2025, 1, 31);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18);
        var barn = new FødselOgDødInfo(AktørId.dummy(), LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 1));
        var input = new BeregnDagsatsInput(perioder, fødselsdag, false, false, List.of(barn));
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(input);

        var forventetTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(
                fom,
                tom,
                lagBarnetilleggSats(0)
            )
        ));

        assertThat(dagsatsTidslinje.resultatTidslinje()).isEqualTo(forventetTidslinje);
    }

    @Test
    void skal_beregne_dagsats_med_ett_barn_levende_hele_perioden() {
        var fom = LocalDate.of(2025, 1, 1);
        var tom = LocalDate.of(2025, 1, 31);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18);
        var barn = new FødselOgDødInfo(AktørId.dummy(), LocalDate.of(2020, 1, 1), null);
        var input = new BeregnDagsatsInput(perioder, fødselsdag, false, false, List.of(barn));
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(input);

        var forventetTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(
                fom,
                tom,
                lagBarnetilleggSats(1)
            )
        ));

        assertThat(dagsatsTidslinje.resultatTidslinje()).isEqualTo(forventetTidslinje);
    }

    @Test
    void skal_beregne_dagsats_med_barn_født_og_død_i_perioden() {
        var fom = LocalDate.of(2025, 1, 1);
        var tom = LocalDate.of(2025, 1, 31);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18);
        var barn = new FødselOgDødInfo(AktørId.dummy(), LocalDate.of(2025, 1, 10), LocalDate.of(2025, 1, 20));
        var input = new BeregnDagsatsInput(perioder, fødselsdag, false, false, List.of(barn));
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(input);

        var forventetTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(
                fom,
                LocalDate.of(2025, 1, 9),
                lagBarnetilleggSats(0)
            ),
            new LocalDateSegment<>(
                LocalDate.of(2025, 1, 10),
                LocalDate.of(2025, 1, 20),
                lagBarnetilleggSats(1)
            ),
            new LocalDateSegment<>(
                LocalDate.of(2025, 1, 21),
                tom,
                lagBarnetilleggSats(0)
            )
        ));

        assertThat(dagsatsTidslinje.resultatTidslinje()).isEqualTo(forventetTidslinje);
    }

    @Test
    void skal_beregne_dagsats_med_flere_barn_med_ulik_varighet() {
        var fom = LocalDate.of(2025, 1, 1);
        var tom = LocalDate.of(2025, 1, 31);
        var perioder = new LocalDateTimeline<>(fom, tom, Boolean.TRUE);
        var fødselsdag = fom.minusYears(18);
        var barn1 = new FødselOgDødInfo(AktørId.dummy(), LocalDate.of(2020, 1, 1), null);
        var barn2 = new FødselOgDødInfo(AktørId.dummy(), LocalDate.of(2025, 1, 15), null);
        var input = new BeregnDagsatsInput(perioder, fødselsdag, false, false, List.of(barn1, barn2));
        var dagsatsTidslinje = UngdomsytelseBeregnDagsats.beregnDagsats(input);

        var forventetTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(
                fom,
                LocalDate.of(2025, 1, 14),
                lagBarnetilleggSats(1)
            ),
            new LocalDateSegment<>(
                LocalDate.of(2025, 1, 15),
                tom,
                lagBarnetilleggSats(2)
            )
        ));

        assertThat(dagsatsTidslinje.resultatTidslinje()).isEqualTo(forventetTidslinje);
    }

    private static UngdomsytelseSatser lagBarnetilleggSats(int antallBarn) {
        return new UngdomsytelseSatser(
            DAGSATS_LAV_SATS, // dagsats
            GRUNNBELØP, // grunnbeløp
            GRUNNBELØP_FAKTOR_LAV_SATS, // grunnbeløpFaktor
            no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType.LAV,
            antallBarn, // antallBarn
            antallBarn*DAGSATS_BARNETILLEGG // dagsatsBarnetillegg
        );
    }

    private static BeregnDagsatsInput lagInput(LocalDateTimeline<Boolean> perioder, LocalDate fødselsdag) {
        boolean harTriggerBeregnHøySats = false;
        return lagInput(perioder, fødselsdag, harTriggerBeregnHøySats);
    }

    private static BeregnDagsatsInput lagInput(LocalDateTimeline<Boolean> perioder, LocalDate fødselsdag, boolean harTriggerBeregnHøySats) {
        return new BeregnDagsatsInput(perioder, fødselsdag, harTriggerBeregnHøySats, false, List.of());
    }
}
