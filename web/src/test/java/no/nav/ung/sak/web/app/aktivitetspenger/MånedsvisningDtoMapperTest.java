package no.nav.ung.sak.web.app.aktivitetspenger;

import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollerteInntekter;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;
import no.nav.ung.sak.kontrakt.aktivitetspenger.beregning.AktivitetspengerSatsType;
import no.nav.ung.sak.kontrakt.ungdomsytelse.ytelse.UtbetalingStatus;
import no.nav.ung.sak.typer.Beløp;
import no.nav.ung.sak.web.app.ungdomsytelse.BehandlingAvsluttetTidspunkt;
import no.nav.ung.ytelse.aktivitetspenger.beregning.AktivitetspengerSatser;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.BeregningInput;
import no.nav.ung.ytelse.aktivitetspenger.beregning.beste.Beregningsgrunnlag;
import no.nav.ung.ytelse.aktivitetspenger.beregning.minstesats.AktivitetspengerSatsGrunnlag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class MånedsvisningDtoMapperTest {

    private static AktivitetspengerSatser lagSatser(BigDecimal dagsats, BigDecimal grunnbeløp, BigDecimal grunnbeløpFaktor, UngdomsytelseSatsType satsType, int antallBarn, int dagsatsBarnetillegg, BigDecimal beregnetRedusertPrAar) {
        var satsGrunnlag = new AktivitetspengerSatsGrunnlag(dagsats, grunnbeløp, grunnbeløpFaktor, grunnbeløpFaktor.multiply(grunnbeløp), satsType, antallBarn, dagsatsBarnetillegg);
        var beregningInput = new BeregningInput(new Beløp(0), new Beløp(0), new Beløp(0), LocalDate.now(), Year.of(2024));
        var beregningsgrunnlag = new Beregningsgrunnlag(beregningInput, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, beregnetRedusertPrAar, "");
        return new AktivitetspengerSatser(satsGrunnlag, beregningsgrunnlag);
    }

    @Test
    void skal_mappe_delvis_måned_uten_kontroll_i_førstegangsbehandling() {
        // Assert
        final var fom = LocalDate.of(2025, 1, 15);
        final var tom = LocalDate.of(2025, 1, 25);

        final var uredusert = BigDecimal.TEN;
        final var reduksjon = BigDecimal.ZERO;
        final var redusert = BigDecimal.TEN;
        final var dagsats = BigDecimal.ONE;
        final var dagsatsBarnetillegg = 37;
        final var antallBarn = 1;
        final var satser = lagSatser(dagsats, BigDecimal.valueOf(100_000), BigDecimal.valueOf(2), UngdomsytelseSatsType.HØY, antallBarn, dagsatsBarnetillegg, BigDecimal.ZERO);

        final var månedstidslinje = new LocalDateTimeline<>(fom, tom, YearMonth.of(2025, 1));
        final var tilkjentYtelseTidslinje = new LocalDateTimeline<>(fom, tom, new TilkjentYtelseVerdi(uredusert, reduksjon, redusert, dagsats, BigDecimal.valueOf(100), BigDecimal.TEN));
        final var kontrollTidslinje = new LocalDateTimeline<KontrollerteInntekter>(List.of());
        final var satsTidslinje = new LocalDateTimeline<>(fom, tom, satser);
        var avsluttetTid = LocalDateTime.now();
        var aktuellAvsluttetTid = new BehandlingAvsluttetTidspunkt(avsluttetTid);

        // Act
        final var månedsvisningDto = MånedsvisningDtoMapper.mapSatsOgUtbetalingPrMåned(
            aktuellAvsluttetTid,
            månedstidslinje,
            tilkjentYtelseTidslinje,
            kontrollTidslinje,
            satsTidslinje,
            Map.of(aktuellAvsluttetTid, tilkjentYtelseTidslinje));

        // Assert
        assertThat(månedsvisningDto.size()).isEqualTo(1);

        final var månedDto = månedsvisningDto.getFirst();

        assertThat(månedDto.måned()).isEqualTo(YearMonth.of(2025, 1));
        assertThat(månedDto.antallDager()).isEqualTo(8);
        assertThat(månedDto.rapportertInntekt()).isNull();
        assertThat(månedDto.utbetaling()).isEqualByComparingTo(redusert);
        assertThat(månedDto.reduksjon()).isEqualByComparingTo(reduksjon);
        assertThat(månedDto.status()).isEqualTo(UtbetalingStatus.UTBETALT);
        assertThat(månedDto.satsperioder().size()).isEqualTo(1);
        final var satsPeriode = månedDto.satsperioder().getFirst();

        assertThat(satsPeriode.dagsatsBarnetillegg()).isEqualTo(dagsatsBarnetillegg);
        assertThat(satsPeriode.antallBarn()).isEqualTo(antallBarn);
        assertThat(satsPeriode.antallDager()).isEqualTo(8);
        assertThat(satsPeriode.fom()).isEqualTo(fom);
        assertThat(satsPeriode.tom()).isEqualTo(tom);
        assertThat(satsPeriode.dagsats()).isEqualTo(dagsats);
        assertThat(satsPeriode.grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));
    }


    @Test
    void skal_mappe_to_måneder_med_kontroll_uten_inntekt_i_revurdering() {
        // Assert
        var fom = LocalDate.of(2025, 1, 15);
        var tom = fom.plusWeeks(51).minusDays(1);

        var uredusert = BigDecimal.TEN;
        var reduksjon = BigDecimal.ZERO;
        var redusert = BigDecimal.TEN;
        var dagsats = BigDecimal.ONE;
        var dagsatsBarnetillegg = 37;
        var antallBarn = 1;
        var satser = lagSatser(dagsats, BigDecimal.valueOf(100_000), BigDecimal.valueOf(2), UngdomsytelseSatsType.HØY, antallBarn, dagsatsBarnetillegg, BigDecimal.ZERO);

        var fomMåned1 = fom;
        var tomMåned1 = fom.with(TemporalAdjusters.lastDayOfMonth());
        var fomMåned2 = tomMåned1.plusDays(1);
        var tomMåned2 = fomMåned2.with(TemporalAdjusters.lastDayOfMonth());
        final var månedstidslinje = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fomMåned1, tomMåned1, YearMonth.of(2025, 1)), new LocalDateSegment<>(fomMåned2, tomMåned2, YearMonth.of(2025, 2))));
        final var tilkjentYtelseTidslinje = new LocalDateTimeline<>(List.of(
            new LocalDateSegment<>(fomMåned1, tomMåned1, new TilkjentYtelseVerdi(uredusert, reduksjon, redusert, dagsats, BigDecimal.valueOf(100), BigDecimal.TEN)),
            new LocalDateSegment<>(fomMåned2, tomMåned2, new TilkjentYtelseVerdi(uredusert, reduksjon, redusert, dagsats, BigDecimal.valueOf(100), BigDecimal.TEN))));
        final var kontrollTidslinje = new LocalDateTimeline<>(fomMåned2, tomMåned2, new KontrollerteInntekter(BigDecimal.ZERO, null));
        final var satsTidslinje = new LocalDateTimeline<>(fom, tom, satser);
        var avsluttetTid = LocalDateTime.now();
        var aktuellAvsluttetTid = new BehandlingAvsluttetTidspunkt(avsluttetTid);

        // Act
        final var månedsvisningDto = MånedsvisningDtoMapper.mapSatsOgUtbetalingPrMåned(
            aktuellAvsluttetTid,
            månedstidslinje,
            tilkjentYtelseTidslinje,
            kontrollTidslinje,
            satsTidslinje,
            Map.of(aktuellAvsluttetTid, tilkjentYtelseTidslinje));

        // Assert
        assertThat(månedsvisningDto.size()).isEqualTo(2);

        final var månedDto1 = månedsvisningDto.get(0);

        assertThat(månedDto1.måned()).isEqualTo(YearMonth.of(2025, 1));
        assertThat(månedDto1.antallDager()).isEqualTo(13);
        assertThat(månedDto1.rapportertInntekt()).isNull();
        assertThat(månedDto1.utbetaling()).isEqualByComparingTo(redusert);
        assertThat(månedDto1.reduksjon()).isEqualByComparingTo(reduksjon);
        assertThat(månedDto1.status()).isEqualTo(UtbetalingStatus.UTBETALT);
        assertThat(månedDto1.satsperioder().size()).isEqualTo(1);
        final var satsPeriode = månedDto1.satsperioder().getFirst();

        assertThat(satsPeriode.dagsatsBarnetillegg()).isEqualTo(dagsatsBarnetillegg);
        assertThat(satsPeriode.antallBarn()).isEqualTo(antallBarn);
        assertThat(satsPeriode.antallDager()).isEqualTo(13);
        assertThat(satsPeriode.fom()).isEqualTo(fomMåned1);
        assertThat(satsPeriode.tom()).isEqualTo(tomMåned1);
        assertThat(satsPeriode.dagsats()).isEqualTo(dagsats);
        assertThat(satsPeriode.grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));


        final var månedDto2 = månedsvisningDto.get(1);

        assertThat(månedDto2.måned()).isEqualTo(YearMonth.of(2025, 2));
        assertThat(månedDto2.antallDager()).isEqualTo(20);
        assertThat(månedDto2.rapportertInntekt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(månedDto2.utbetaling()).isEqualByComparingTo(redusert);
        assertThat(månedDto2.reduksjon()).isEqualByComparingTo(reduksjon);
        assertThat(månedDto2.status()).isEqualTo(UtbetalingStatus.UTBETALT);
        assertThat(månedDto2.satsperioder().size()).isEqualTo(1);
        final var satsPeriodeMåned2 = månedDto2.satsperioder().getFirst();

        assertThat(satsPeriodeMåned2.dagsatsBarnetillegg()).isEqualTo(dagsatsBarnetillegg);
        assertThat(satsPeriodeMåned2.antallBarn()).isEqualTo(antallBarn);
        assertThat(satsPeriodeMåned2.antallDager()).isEqualTo(20);
        assertThat(satsPeriodeMåned2.fom()).isEqualTo(fomMåned2);
        assertThat(satsPeriodeMåned2.tom()).isEqualTo(tomMåned2);
        assertThat(satsPeriodeMåned2.dagsats()).isEqualTo(dagsats);
        assertThat(satsPeriodeMåned2.grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));

    }

    @Test
    void skal_mappe_to_måneder_med_kontroll_uten_inntekt_i_revurdering_endring_av_sats_midt_i_andre_måned() {
        // Assert
        final var fom = LocalDate.of(2025, 1, 15);
        final var tom = fom.plusWeeks(51).minusDays(1);

        var uredusert = BigDecimal.TEN;
        var reduksjon = BigDecimal.ZERO;
        var redusert = BigDecimal.TEN;
        var dagsats = BigDecimal.ONE;
        var dagsatsBarnetillegg = 37;
        var antallBarn = 1;
        var satser = lagSatser(dagsats, BigDecimal.valueOf(100_000), BigDecimal.valueOf(2), UngdomsytelseSatsType.HØY, antallBarn, dagsatsBarnetillegg, BigDecimal.ZERO);
        var dagsatsBarnetillegg2 = 74;
        var antallBarn2 = 2;
        var satser2 = lagSatser(dagsats, BigDecimal.valueOf(100_000), BigDecimal.valueOf(2), UngdomsytelseSatsType.HØY, antallBarn2, dagsatsBarnetillegg2, BigDecimal.ZERO);

        var fomMåned1 = fom;
        var tomMåned1 = fom.with(TemporalAdjusters.lastDayOfMonth());
        var fomMåned2 = tomMåned1.plusDays(1);
        var tomMåned2 = fomMåned2.with(TemporalAdjusters.lastDayOfMonth());
        final var satsendringDato = fomMåned2.plusDays(11);

        final var månedstidslinje = new LocalDateTimeline<YearMonth>(List.of(
            new LocalDateSegment<>(fomMåned1, tomMåned1, YearMonth.of(2025, 1)),
            new LocalDateSegment<>(fomMåned2, tomMåned2, YearMonth.of(2025, 2))));
        final var tilkjentYtelseTidslinje = new LocalDateTimeline<TilkjentYtelseVerdi>(List.of(
            new LocalDateSegment<>(fomMåned1, tomMåned1, new TilkjentYtelseVerdi(uredusert, reduksjon, redusert, dagsats, BigDecimal.valueOf(100), BigDecimal.TEN)),
            new LocalDateSegment<>(fomMåned2, tomMåned2, new TilkjentYtelseVerdi(uredusert, reduksjon, redusert, dagsats, BigDecimal.valueOf(100), BigDecimal.TEN))));
        final var kontrollTidslinje = new LocalDateTimeline<KontrollerteInntekter>(fomMåned2, tomMåned2, new KontrollerteInntekter(BigDecimal.ZERO, null));
        final var satsTidslinje = new LocalDateTimeline<AktivitetspengerSatser>(List.of(
            new LocalDateSegment<>(fom, fomMåned2.plusDays(10), satser),
            new LocalDateSegment<>(satsendringDato, tom, satser2)));
        var avsluttetTid = LocalDateTime.now();
        var aktuellAvsluttetTid = new BehandlingAvsluttetTidspunkt(avsluttetTid);


        // Act
        final var månedsvisningDto = MånedsvisningDtoMapper.mapSatsOgUtbetalingPrMåned(
            aktuellAvsluttetTid,
            månedstidslinje,
            tilkjentYtelseTidslinje,
            kontrollTidslinje,
            satsTidslinje,
            Map.of(aktuellAvsluttetTid, tilkjentYtelseTidslinje));

        // Assert
        assertThat(månedsvisningDto.size()).isEqualTo(2);

        final var månedDto1 = månedsvisningDto.get(0);

        assertThat(månedDto1.måned()).isEqualTo(YearMonth.of(2025, 1));
        assertThat(månedDto1.antallDager()).isEqualTo(13);
        assertThat(månedDto1.rapportertInntekt()).isNull();
        assertThat(månedDto1.utbetaling()).isEqualByComparingTo(redusert);
        assertThat(månedDto1.reduksjon()).isEqualByComparingTo(reduksjon);
        assertThat(månedDto1.status()).isEqualTo(UtbetalingStatus.UTBETALT);
        assertThat(månedDto1.satsperioder().size()).isEqualTo(1);
        final var satsPeriode = månedDto1.satsperioder().getFirst();

        assertThat(satsPeriode.dagsatsBarnetillegg()).isEqualTo(dagsatsBarnetillegg);
        assertThat(satsPeriode.antallBarn()).isEqualTo(antallBarn);
        assertThat(satsPeriode.antallDager()).isEqualTo(13);
        assertThat(satsPeriode.fom()).isEqualTo(fomMåned1);
        assertThat(satsPeriode.tom()).isEqualTo(tomMåned1);
        assertThat(satsPeriode.dagsats()).isEqualTo(dagsats);
        assertThat(satsPeriode.grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));


        final var månedDto2 = månedsvisningDto.get(1);

        assertThat(månedDto2.måned()).isEqualTo(YearMonth.of(2025, 2));
        assertThat(månedDto2.antallDager()).isEqualTo(20);
        assertThat(månedDto2.rapportertInntekt()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(månedDto2.utbetaling()).isEqualByComparingTo(redusert);
        assertThat(månedDto2.reduksjon()).isEqualByComparingTo(reduksjon);
        assertThat(månedDto2.status()).isEqualTo(UtbetalingStatus.UTBETALT);
        assertThat(månedDto2.satsperioder().size()).isEqualTo(2);
        final var satsPeriode1Måned2 = månedDto2.satsperioder().get(0);

        assertThat(satsPeriode1Måned2.dagsatsBarnetillegg()).isEqualTo(dagsatsBarnetillegg);
        assertThat(satsPeriode1Måned2.antallBarn()).isEqualTo(antallBarn);
        assertThat(satsPeriode1Måned2.antallDager()).isEqualTo(7);
        assertThat(satsPeriode1Måned2.fom()).isEqualTo(fomMåned2);
        assertThat(satsPeriode1Måned2.tom()).isEqualTo(satsendringDato.minusDays(1));
        assertThat(satsPeriode1Måned2.dagsats()).isEqualTo(dagsats);
        assertThat(satsPeriode1Måned2.grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));


        final var satsPeriode2Måned2 = månedDto2.satsperioder().get(1);
        assertThat(satsPeriode2Måned2.dagsatsBarnetillegg()).isEqualTo(dagsatsBarnetillegg2);
        assertThat(satsPeriode2Måned2.antallBarn()).isEqualTo(antallBarn2);
        assertThat(satsPeriode2Måned2.antallDager()).isEqualTo(13);
        assertThat(satsPeriode2Måned2.fom()).isEqualTo(satsendringDato);
        assertThat(satsPeriode2Måned2.tom()).isEqualTo(tomMåned2);
        assertThat(satsPeriode2Måned2.dagsats()).isEqualTo(dagsats);
        assertThat(satsPeriode2Måned2.grunnbeløpFaktor()).isEqualByComparingTo(BigDecimal.valueOf(2));
    }

    @Test
    void skal_returnere_beregningsgrunnlag_som_satstype_når_beregning_er_høyere_enn_minsteytelse() {
        // Assert
        final var fom = LocalDate.of(2025, 1, 15);
        final var tom = LocalDate.of(2025, 1, 25);

        final var uredusert = BigDecimal.TEN;
        final var reduksjon = BigDecimal.ZERO;
        final var redusert = BigDecimal.TEN;
        final var dagsatsBarnetillegg = 0;
        final var antallBarn = 0;
        final var grunnbeløp = BigDecimal.valueOf(124_028);
        final var grunnbeløpFaktor = BigDecimal.valueOf(2);
        // 280_000 (beregnetRedusertPrAar) > 248_056 (minsteytelse = 124_028 * 2), gir BEREGNINGSGRUNNLAG
        final var beregnetRedusertPrAar = BigDecimal.valueOf(280_000);
        final var forventetDagsats = BigDecimal.valueOf(1077);

        final var satser = lagSatser(BigDecimal.ONE, grunnbeløp, grunnbeløpFaktor, UngdomsytelseSatsType.HØY, antallBarn, dagsatsBarnetillegg, beregnetRedusertPrAar);

        final var månedstidslinje = new LocalDateTimeline<>(fom, tom, YearMonth.of(2025, 1));
        final var tilkjentYtelseTidslinje = new LocalDateTimeline<>(fom, tom, new TilkjentYtelseVerdi(uredusert, reduksjon, redusert, forventetDagsats, BigDecimal.valueOf(100), BigDecimal.TEN));
        final var kontrollTidslinje = new LocalDateTimeline<KontrollerteInntekter>(List.of());
        final var satsTidslinje = new LocalDateTimeline<>(fom, tom, satser);
        var avsluttetTid = LocalDateTime.now();
        var aktuellAvsluttetTid = new BehandlingAvsluttetTidspunkt(avsluttetTid);

        // Act
        final var månedsvisningDto = MånedsvisningDtoMapper.mapSatsOgUtbetalingPrMåned(
            aktuellAvsluttetTid,
            månedstidslinje,
            tilkjentYtelseTidslinje,
            kontrollTidslinje,
            satsTidslinje,
            Map.of(aktuellAvsluttetTid, tilkjentYtelseTidslinje));

        // Assert
        assertThat(månedsvisningDto.size()).isEqualTo(1);

        final var satsPeriode = månedsvisningDto.getFirst().satsperioder().getFirst();
        assertThat(satsPeriode.satsType()).isEqualTo(AktivitetspengerSatsType.BEREGNINGSGRUNNLAG);
        assertThat(satsPeriode.dagsats()).isEqualByComparingTo(forventetDagsats);
        assertThat(satsPeriode.grunnbeløpFaktor()).isEqualByComparingTo(grunnbeløpFaktor);
        assertThat(satsPeriode.antallBarn()).isEqualTo(antallBarn);
        assertThat(satsPeriode.dagsatsBarnetillegg()).isEqualTo(dagsatsBarnetillegg);
    }
}
