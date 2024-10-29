package no.nav.k9.sak.ytelse.pleiepengerbarn.beregningsgrunnlag;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.folketrygdloven.kalkulus.felles.v1.Aktivitetsgrad;
import no.nav.folketrygdloven.kalkulus.felles.v1.Utbetalingsgrad;
import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.sak.domene.iay.modell.YrkesaktivitetBuilder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.typer.Arbeidsgiver;
import no.nav.pleiepengerbarn.uttak.kontrakter.AnnenPart;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeid;
import no.nav.pleiepengerbarn.uttak.kontrakter.Arbeidsforhold;
import no.nav.pleiepengerbarn.uttak.kontrakter.ArbeidsforholdPeriodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.LukketPeriode;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utbetalingsgrader;
import no.nav.pleiepengerbarn.uttak.kontrakter.Utfall;
import no.nav.pleiepengerbarn.uttak.kontrakter.UttaksperiodeInfo;
import no.nav.pleiepengerbarn.uttak.kontrakter.Uttaksplan;

class MapTilUtbetalingsgradPrAktivitetTest {

    private static final String ORGNR = "974760673";
    private static final Arbeidsgiver VIRKSOMHET = Arbeidsgiver.virksomhet(ORGNR);


    @Test
    void skal_mappe_utbetalingsgrader_med_frilans_i_søknad_og_uttak() {

        // Arrange
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var periode = new LukketPeriode(fom, tom);
        var frilansArbeidstid = lagArbeidstidForFrilans(fom, tom);
        var uttaksplan = lagUttaksplanMedFrilans(periode, 50);

        // Act
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            uttaksplan,
            List.of(frilansArbeidstid),
            List.of(lagFrilansYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom.plusDays(1)))));

        // Assert
        assertThat(utbetalingsgrader.size()).isEqualTo(1);

        var utbetalingsgradPrAktivitetDto = utbetalingsgrader.get(0);
        var aktivitet = utbetalingsgradPrAktivitetDto.getUtbetalingsgradArbeidsforholdDto();
        assertThat(aktivitet.getUttakArbeidType()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType.FRILANS);
        assertThat(aktivitet.getArbeidsgiver()).isNull();
        var perioderMedUtbetalingsgrad = utbetalingsgradPrAktivitetDto.getPeriodeMedUtbetalingsgrad();
        assertThat(perioderMedUtbetalingsgrad.size()).isEqualTo(1);
        var periodeMedUtbetalingsgrad = perioderMedUtbetalingsgrad.get(0);
        assertThat(periodeMedUtbetalingsgrad.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.fra(50));
        assertThat(periodeMedUtbetalingsgrad.getAktivitetsgrad()).isEqualTo(Aktivitetsgrad.fra(50));
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getFom()).isEqualTo(fom);
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getTom()).isEqualTo(tom);
    }


    @Test
    void skal_mappe_utbetalingsgrader_med_frilans_i_søknad_og_uttak_der_deler_av_perioden_er_0_over_0() {

        // Arrange
        var fom = LocalDate.now();
        var fomFrilansUttak = fom.plusDays(2);
        var tom = LocalDate.now().plusDays(10);
        var frilansNullOverNullArbeidstid = lagNullOverNullArbeidstidForFrilans(fom, fomFrilansUttak.minusDays(1));
        var frilansArbeidstid = lagArbeidstidForFrilans(fomFrilansUttak, tom);
        var perioder = Map.of(
            new LukketPeriode(fom, fomFrilansUttak.minusDays(1)),
            lagPeriodeInfo(List.of(lagUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, ORGNR, 50))),
            new LukketPeriode(fomFrilansUttak, tom),
            lagPeriodeInfo(List.of(
                lagUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, ORGNR, 50),
                lagUtbetalingsgrad(UttakArbeidType.FRILANSER, null, 30))));
        var uttaksplan = lagUttaksplan(perioder);

        // Act
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            uttaksplan,
            List.of(frilansNullOverNullArbeidstid, frilansArbeidstid),
            List.of(lagFrilansYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom.plusDays(1)))));

        // Assert
        assertThat(utbetalingsgrader.size()).isEqualTo(2);

        var frilansUtbetalingsgradPrAktivitetDto = utbetalingsgrader.get(1);
        var frilansAktivitet = frilansUtbetalingsgradPrAktivitetDto.getUtbetalingsgradArbeidsforholdDto();
        assertThat(frilansAktivitet.getUttakArbeidType()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType.FRILANS);
        assertThat(frilansAktivitet.getArbeidsgiver()).isNull();
        var frilansPerioderMedUtbetalingsgrad = frilansUtbetalingsgradPrAktivitetDto.getPeriodeMedUtbetalingsgrad();
        assertThat(frilansPerioderMedUtbetalingsgrad.size()).isEqualTo(2);
        var frilansPeriodeMedUtbetalingsgrad1 = frilansPerioderMedUtbetalingsgrad.get(0);
        assertThat(frilansPeriodeMedUtbetalingsgrad1.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.fra(0));
        assertThat(frilansPeriodeMedUtbetalingsgrad1.getAktivitetsgrad()).isEqualTo(Aktivitetsgrad.fra(100));
        assertThat(frilansPeriodeMedUtbetalingsgrad1.getPeriode().getFom()).isEqualTo(fom);
        assertThat(frilansPeriodeMedUtbetalingsgrad1.getPeriode().getTom()).isEqualTo(fomFrilansUttak.minusDays(1));

        var frilansPeriodeMedUtbetalingsgrad2 = frilansPerioderMedUtbetalingsgrad.get(1);
        assertThat(frilansPeriodeMedUtbetalingsgrad2.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.fra(30));
        assertThat(frilansPeriodeMedUtbetalingsgrad2.getAktivitetsgrad()).isEqualTo(Aktivitetsgrad.fra(50));
        assertThat(frilansPeriodeMedUtbetalingsgrad2.getPeriode().getFom()).isEqualTo(fomFrilansUttak);
        assertThat(frilansPeriodeMedUtbetalingsgrad2.getPeriode().getTom()).isEqualTo(tom);


    }


    @Test
    void skal_mappe_0_over_0_frilans_fra_søknad_om_det_finnes_yrkesaktivitet() {

        // Arrange
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var periode = new LukketPeriode(fom, tom);
        var frilansArbeidstid = lagNullOverNullArbeidstidForFrilans(fom, tom);
        var fomDatoFrilans = fom.plusDays(1);
        var uttaksplan = lagUttaksplanMedArbeid(periode, 50);

        // Act
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            uttaksplan,
            List.of(frilansArbeidstid),
            List.of(lagFrilansYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(fomDatoFrilans, tom.plusDays(1)))));

        // Assert
        assertThat(utbetalingsgrader.size()).isEqualTo(2);

        var utbetalingsgradPrAktivitetDto = utbetalingsgrader.get(0);
        var aktivitet = utbetalingsgradPrAktivitetDto.getUtbetalingsgradArbeidsforholdDto();
        assertThat(aktivitet.getUttakArbeidType()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType.FRILANS);
        assertThat(aktivitet.getArbeidsgiver()).isNull();
        var perioderMedUtbetalingsgrad = utbetalingsgradPrAktivitetDto.getPeriodeMedUtbetalingsgrad();
        assertThat(perioderMedUtbetalingsgrad.size()).isEqualTo(1);
        var periodeMedUtbetalingsgrad = perioderMedUtbetalingsgrad.get(0);
        assertThat(periodeMedUtbetalingsgrad.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.fra(0));
        assertThat(periodeMedUtbetalingsgrad.getAktivitetsgrad()).isEqualTo(Aktivitetsgrad.fra(100));
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getFom()).isEqualTo(fomDatoFrilans);
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getTom()).isEqualTo(tom);

        var utbetalingsgradPrAktivitetDto2 = utbetalingsgrader.get(1);
        var aktivitet2 = utbetalingsgradPrAktivitetDto2.getUtbetalingsgradArbeidsforholdDto();
        assertThat(aktivitet2.getUttakArbeidType()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType.ORDINÆRT_ARBEID);
        assertThat(aktivitet2.getArbeidsgiver().getIdent()).isEqualTo(ORGNR);
        var perioderMedUtbetalingsgrad2 = utbetalingsgradPrAktivitetDto2.getPeriodeMedUtbetalingsgrad();
        assertThat(perioderMedUtbetalingsgrad2.size()).isEqualTo(1);
        var periodeMedUtbetalingsgrad2 = perioderMedUtbetalingsgrad2.get(0);
        assertThat(periodeMedUtbetalingsgrad2.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.fra(50));
        assertThat(periodeMedUtbetalingsgrad2.getAktivitetsgrad()).isEqualTo(Aktivitetsgrad.fra(50));
        assertThat(periodeMedUtbetalingsgrad2.getPeriode().getFom()).isEqualTo(fom);
        assertThat(periodeMedUtbetalingsgrad2.getPeriode().getTom()).isEqualTo(tom);

    }

    @Test
    void skal_ikke_mappe_0_over_0_frilans_fra_søknad_om_det_ikke_finnes_yrkesaktivitet() {
        // Arrange
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var periode = new LukketPeriode(fom, tom);
        var uttaksplan = lagUttaksplanMedArbeid(periode, 50);
        var frilansArbeidstid = lagNullOverNullArbeidstidForFrilans(fom, tom);

        // ACt
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            uttaksplan,
            List.of(frilansArbeidstid),
            List.of());

        // Assert
        assertThat(utbetalingsgrader.size()).isEqualTo(1);
        assertThat(utbetalingsgrader.size()).isEqualTo(1);
        var utbetalingsgradPrAktivitetDto = utbetalingsgrader.get(0);
        var aktivitet = utbetalingsgradPrAktivitetDto.getUtbetalingsgradArbeidsforholdDto();
        assertThat(aktivitet.getUttakArbeidType()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType.ORDINÆRT_ARBEID);
        assertThat(aktivitet.getArbeidsgiver().getIdent()).isEqualTo(ORGNR);
        var perioderMedUtbetalingsgrad = utbetalingsgradPrAktivitetDto.getPeriodeMedUtbetalingsgrad();
        assertThat(perioderMedUtbetalingsgrad.size()).isEqualTo(1);
        var periodeMedUtbetalingsgrad = perioderMedUtbetalingsgrad.get(0);
        assertThat(periodeMedUtbetalingsgrad.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.fra(50));
        assertThat(periodeMedUtbetalingsgrad.getAktivitetsgrad()).isEqualTo(Aktivitetsgrad.fra(50));
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getFom()).isEqualTo(fom);
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getTom()).isEqualTo(tom);
    }

    @Test
    void skal_mappe_arbeidsaktivitet_fra_uttaksplan() {
        // Arrange
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var periode = new LukketPeriode(fom, tom);
        var arbeidstidArbeid = lagArbeidstidForArbeid(periode);
        var uttaksplan = lagUttaksplanMedArbeid(periode, 50);

        // Act
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            uttaksplan,
            List.of(arbeidstidArbeid),
            List.of(lagArbeidYrkesaktivitet(fom)));

        // Assert
        assertThat(utbetalingsgrader.size()).isEqualTo(1);
        var utbetalingsgradPrAktivitetDto = utbetalingsgrader.get(0);
        var aktivitet = utbetalingsgradPrAktivitetDto.getUtbetalingsgradArbeidsforholdDto();
        assertThat(aktivitet.getUttakArbeidType()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType.ORDINÆRT_ARBEID);
        assertThat(aktivitet.getArbeidsgiver().getIdent()).isEqualTo(ORGNR);
        var perioderMedUtbetalingsgrad = utbetalingsgradPrAktivitetDto.getPeriodeMedUtbetalingsgrad();
        assertThat(perioderMedUtbetalingsgrad.size()).isEqualTo(1);
        var periodeMedUtbetalingsgrad = perioderMedUtbetalingsgrad.get(0);
        assertThat(periodeMedUtbetalingsgrad.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.fra(50));
        assertThat(periodeMedUtbetalingsgrad.getAktivitetsgrad()).isEqualTo(Aktivitetsgrad.fra(50));
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getFom()).isEqualTo(fom);
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getTom()).isEqualTo(tom);
    }

    @Test
    void skal_mappe_arbeidsaktivitet_fra_uttaksplan_der_utbetalingsgrad_fra_uttak_er_ulik_oppgitt_arbeidstid() {
        // Arrange
        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);
        var periode = new LukketPeriode(fom, tom);
        var arbeidstidArbeid = lagArbeidstidForArbeid(periode);
        var uttaksplan = lagUttaksplanMedArbeid(periode, 30);

        // Act
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            uttaksplan,
            List.of(arbeidstidArbeid),
            List.of(lagArbeidYrkesaktivitet(fom)));

        // Assert
        assertThat(utbetalingsgrader.size()).isEqualTo(1);
        var utbetalingsgradPrAktivitetDto = utbetalingsgrader.get(0);
        var aktivitet = utbetalingsgradPrAktivitetDto.getUtbetalingsgradArbeidsforholdDto();
        assertThat(aktivitet.getUttakArbeidType()).isEqualTo(no.nav.folketrygdloven.kalkulus.kodeverk.UttakArbeidType.ORDINÆRT_ARBEID);
        assertThat(aktivitet.getArbeidsgiver().getIdent()).isEqualTo(ORGNR);
        var perioderMedUtbetalingsgrad = utbetalingsgradPrAktivitetDto.getPeriodeMedUtbetalingsgrad();
        assertThat(perioderMedUtbetalingsgrad.size()).isEqualTo(1);
        var periodeMedUtbetalingsgrad = perioderMedUtbetalingsgrad.get(0);
        assertThat(periodeMedUtbetalingsgrad.getUtbetalingsgrad()).isEqualTo(Utbetalingsgrad.fra(30));
        assertThat(periodeMedUtbetalingsgrad.getAktivitetsgrad()).isEqualTo(Aktivitetsgrad.fra(50));
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getFom()).isEqualTo(fom);
        assertThat(periodeMedUtbetalingsgrad.getPeriode().getTom()).isEqualTo(tom);
    }


    private static Arbeid lagNullOverNullArbeidstidForFrilans(LocalDate fom, LocalDate tom) {
        return new Arbeid(new Arbeidsforhold(UttakArbeidType.FRILANSER.getKode(), null, null, null), Map.of(new LukketPeriode(fom, tom), new ArbeidsforholdPeriodeInfo(Duration.ZERO, Duration.ZERO)));
    }

    private static Arbeid lagArbeidstidForFrilans(LocalDate fom, LocalDate tom) {
        return new Arbeid(new Arbeidsforhold(UttakArbeidType.FRILANSER.getKode(), null, null, null), Map.of(new LukketPeriode(fom, tom), new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(3))));
    }


    private static Arbeid lagArbeidstidForArbeid(LukketPeriode periode) {
        return new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), ORGNR, null, null),
            Map.of(periode, new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(4))));
    }

    private static Uttaksplan lagUttaksplanMedArbeid(LukketPeriode periode, int utbetalingsgrad) {
        var utbetalingsgradArbeid = lagUtbetalingsgrad(UttakArbeidType.ARBEIDSTAKER, ORGNR, utbetalingsgrad);
        return lagUttaksplan(periode, List.of(utbetalingsgradArbeid));
    }

    private static Uttaksplan lagUttaksplan(LukketPeriode periode, List<Utbetalingsgrader> utbetalingsgrader) {
        var uttaksperiodeInfo = lagPeriodeInfo(utbetalingsgrader);
        return lagUttaksplan(Map.of(periode, uttaksperiodeInfo));
    }

    private static Uttaksplan lagUttaksplan(Map<LukketPeriode, UttaksperiodeInfo> perioder) {
        return new Uttaksplan(
            perioder, List.of());
    }

    private static UttaksperiodeInfo lagPeriodeInfo(List<Utbetalingsgrader> utbetalingsgrader) {
        return new UttaksperiodeInfo(Utfall.OPPFYLT, BigDecimal.valueOf(100),
            null, null,
            utbetalingsgrader,
            BigDecimal.valueOf(50),
            Duration.ofHours(4),
            Set.of(),
            Map.of(),
            BigDecimal.valueOf(100),
            null,
            Set.of(),
            "",
            AnnenPart.ALENE,
            null,
            null,
            null,
            false,
            null,
            false

        );
    }

    private static Utbetalingsgrader lagUtbetalingsgrad(UttakArbeidType arbeidstaker, String orgnr, int utbetalingsgrad) {
        return new Utbetalingsgrader(new Arbeidsforhold(arbeidstaker.getKode(), orgnr, null, null), Duration.ofHours(8), Duration.ofHours(4), BigDecimal.valueOf(utbetalingsgrad));
    }

    private static Uttaksplan lagUttaksplanMedFrilans(LukketPeriode periode, int utbetalingsgrad) {
        return lagUttaksplan(periode, List.of(lagUtbetalingsgrad(UttakArbeidType.FRILANSER, null, utbetalingsgrad)));
    }

    private Yrkesaktivitet lagArbeidYrkesaktivitet(LocalDate fom) {
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(fom.minusYears(1), fom.plusYears(1)));

        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.ORDINÆRT_ARBEIDSFORHOLD)
            .medArbeidsgiver(VIRKSOMHET)
            .leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);

        var yrkesaktivitet = yrkesaktivitetBuilder.build();
        return yrkesaktivitet;
    }

    private Yrkesaktivitet lagFrilansYrkesaktivitet(DatoIntervallEntitet periode) {
        var yrkesaktivitetBuilder = YrkesaktivitetBuilder.oppdatere(Optional.empty());
        var aktivitetsAvtaleBuilder = yrkesaktivitetBuilder.getAktivitetsAvtaleBuilder()
            .medPeriode(periode);

        yrkesaktivitetBuilder
            .medArbeidType(ArbeidType.FRILANSER_OPPDRAGSTAKER_MED_MER)
            .medArbeidsgiver(VIRKSOMHET)
            .leggTilAktivitetsAvtale(aktivitetsAvtaleBuilder);

        var yrkesaktivitet = yrkesaktivitetBuilder.build();
        return yrkesaktivitet;
    }
}
