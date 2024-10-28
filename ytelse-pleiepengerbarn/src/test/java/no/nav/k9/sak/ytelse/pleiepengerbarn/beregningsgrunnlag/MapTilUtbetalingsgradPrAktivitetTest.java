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
    void skal_ikke_mappe_utbetalingsgrader_dersom_ingen_uttaksplan_og_ingen_aktiviteter_fra_søknad() {

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);

        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            Optional.empty(),
            List.of(),
            List.of(lagArbeidYrkesaktivitet(fom)));


        assertThat(utbetalingsgrader.isEmpty()).isTrue();
    }

    @Test
    void skal_mappe_0_over_0_frilans_fra_søknad_om_det_finnes_yrkesaktivitet() {

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);

        var frilansArbeidstid = lagArbeidstidForFrilans(fom, tom);
        var fomDatoFrilans = fom.plusDays(1);
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            Optional.empty(),
            List.of(frilansArbeidstid),
            List.of(lagFrilansYrkesaktivitet(DatoIntervallEntitet.fraOgMedTilOgMed(fomDatoFrilans, tom.plusDays(1)))));


        assertThat(utbetalingsgrader.size()).isEqualTo(1);
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
    }

    @Test
    void skal_ikke_mappe_0_over_0_frilans_fra_søknad_om_det_ikke_finnes_yrkesaktivitet() {

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);

        var frilansArbeidstid = lagArbeidstidForFrilans(fom, tom);
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            Optional.empty(),
            List.of(frilansArbeidstid),
            List.of());

        assertThat(utbetalingsgrader.size()).isEqualTo(0);
    }

    @Test
    void skal_mappe_arbeidsaktivitet_fra_uttaksplan_og_søknad() {

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);

        var periode = new LukketPeriode(fom, tom);
        var arbeidstidArbeid = lagArbeidstidForArbeid(periode);
        var uttaksplan = lagUttaksplanMedArbeid(periode, 50);
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            Optional.of(uttaksplan),
            List.of(arbeidstidArbeid),
            List.of(lagArbeidYrkesaktivitet(fom)));


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
    void skal_mappe_arbeidsaktivitet_fra_uttaksplan_og_søknad_der_utbetalingsgrad_fra_uttak_er_ulik_søknad() {

        var fom = LocalDate.now();
        var tom = LocalDate.now().plusDays(10);

        var periode = new LukketPeriode(fom, tom);
        var arbeidstidArbeid = lagArbeidstidForArbeid(periode);
        var uttaksplan = lagUttaksplanMedArbeid(periode, 30);
        var utbetalingsgrader = MapTilUtbetalingsgradPrAktivitet.finnUtbetalingsgraderOgAktivitetsgrader(DatoIntervallEntitet.fra(fom, tom),
            Optional.of(uttaksplan),
            List.of(arbeidstidArbeid),
            List.of(lagArbeidYrkesaktivitet(fom)));


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


    private static Arbeid lagArbeidstidForFrilans(LocalDate fom, LocalDate tom) {
        return new Arbeid(new Arbeidsforhold(UttakArbeidType.FRILANSER.getKode(), null, null, null), Map.of(new LukketPeriode(fom, tom), new ArbeidsforholdPeriodeInfo(Duration.ZERO, Duration.ZERO)));
    }

    private static Arbeid lagArbeidstidForArbeid(LukketPeriode periode) {
        return new Arbeid(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), ORGNR, null, null),
            Map.of(periode, new ArbeidsforholdPeriodeInfo(Duration.ofHours(8), Duration.ofHours(4))));
    }

    private static Uttaksplan lagUttaksplanMedArbeid(LukketPeriode periode, int utbetalingsgrad) {
        var uttaksperiodeInfo = new UttaksperiodeInfo(Utfall.OPPFYLT, BigDecimal.valueOf(100), null, null,
            List.of(new Utbetalingsgrader(new Arbeidsforhold(UttakArbeidType.ARBEIDSTAKER.getKode(), ORGNR, null, null), Duration.ofHours(8), Duration.ofHours(4), BigDecimal.valueOf(utbetalingsgrad))),
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
        var uttaksplan = new Uttaksplan(
            Map.of(periode, uttaksperiodeInfo
            ), List.of());
        return uttaksplan;
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
