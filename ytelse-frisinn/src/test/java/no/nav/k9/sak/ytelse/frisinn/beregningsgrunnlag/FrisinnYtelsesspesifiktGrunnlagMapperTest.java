package no.nav.k9.sak.ytelse.frisinn.beregningsgrunnlag;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import no.nav.folketrygdloven.kalkulus.beregning.v1.PeriodeMedSøkerInfoDto;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;

public class FrisinnYtelsesspesifiktGrunnlagMapperTest {


    @Test
    public void skal_mappe_frisinn_rikitg_inn() {
        FrisinnYtelsesspesifiktGrunnlagMapper frisinnYtelsesspesifiktGrunnlagMapper = new FrisinnYtelsesspesifiktGrunnlagMapper();
        LocalDate dato3 = LocalDate.of(2020, 4, 30);

        LocalDate dato1 = LocalDate.of(2020, 4, 15);
        LocalDate dato2 = LocalDate.of(2020, 4, 25);

        UttakAktivitetPeriode uttakAktivitetPeriode = new UttakAktivitetPeriode(dato1, dato3, UttakArbeidType.FRILANSER, null, null);
        UttakAktivitetPeriode uttakAktivitetPeriode2 = new UttakAktivitetPeriode(dato2, dato3, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null);

        UttakAktivitet uttakAktivitet = new UttakAktivitet(List.of(uttakAktivitetPeriode, uttakAktivitetPeriode2));

        List<PeriodeMedSøkerInfoDto> periodeMedSøkerInfoDtos = frisinnYtelsesspesifiktGrunnlagMapper.mapPeriodeMedSøkerInfoDto(uttakAktivitet);

        periodeMedSøkerInfoDtos.sort(Comparator.comparing(o -> o.getPeriode().getFom()));

        assertThat(periodeMedSøkerInfoDtos).hasSize(2);
        assertThat(periodeMedSøkerInfoDtos.get(0).getPeriode().getFom()).isEqualTo(dato1);
        assertThat(periodeMedSøkerInfoDtos.get(0).getPeriode().getTom()).isEqualTo(dato2.minusDays(1));
        assertThat(periodeMedSøkerInfoDtos.get(0).getSøkerFrilansIPeriode()).isTrue();
        assertThat(periodeMedSøkerInfoDtos.get(0).getSøkerNæringIPeriode()).isFalse();

        assertThat(periodeMedSøkerInfoDtos.get(1).getPeriode().getFom()).isEqualTo(dato2);
        assertThat(periodeMedSøkerInfoDtos.get(1).getPeriode().getTom()).isEqualTo(dato3);
        assertThat(periodeMedSøkerInfoDtos.get(1).getSøkerFrilansIPeriode()).isTrue();
        assertThat(periodeMedSøkerInfoDtos.get(1).getSøkerNæringIPeriode()).isTrue();
    }

    @Test
    public void skal_mappe_frisinngrunnlag_rikitg_inn_kompleks() {
        FrisinnYtelsesspesifiktGrunnlagMapper frisinnYtelsesspesifiktGrunnlagMapper = new FrisinnYtelsesspesifiktGrunnlagMapper();
        LocalDate sluttenIApril = LocalDate.of(2020, 4, 30);
        LocalDate sluttenIMai = LocalDate.of(2020, 5, 31);

        LocalDate startFLIApril = LocalDate.of(2020, 4, 15);
        LocalDate startSNIApril = LocalDate.of(2020, 4, 25);
        LocalDate startFLIMai = LocalDate.of(2020, 5, 2);
        LocalDate startSNIMai = LocalDate.of(2020, 5, 10);

        UttakAktivitetPeriode uttakAktivitetPeriode = new UttakAktivitetPeriode(startFLIApril, sluttenIApril, UttakArbeidType.FRILANSER, null, null);
        UttakAktivitetPeriode uttakAktivitetPeriode2 = new UttakAktivitetPeriode(startSNIApril, sluttenIApril, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null);
        UttakAktivitetPeriode uttakAktivitetPeriode3 = new UttakAktivitetPeriode(startFLIMai, sluttenIMai, UttakArbeidType.FRILANSER, null, null);
        UttakAktivitetPeriode uttakAktivitetPeriode4 = new UttakAktivitetPeriode(startSNIMai, sluttenIMai, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null);

        UttakAktivitet uttakAktivitet = new UttakAktivitet(List.of(uttakAktivitetPeriode, uttakAktivitetPeriode2,uttakAktivitetPeriode3, uttakAktivitetPeriode4));

        List<PeriodeMedSøkerInfoDto> periodeMedSøkerInfoDtos = frisinnYtelsesspesifiktGrunnlagMapper.mapPeriodeMedSøkerInfoDto(uttakAktivitet);

        periodeMedSøkerInfoDtos.sort(Comparator.comparing(o -> o.getPeriode().getFom()));

        assertThat(periodeMedSøkerInfoDtos).hasSize(4);
        assertThat(periodeMedSøkerInfoDtos.get(0).getPeriode().getFom()).isEqualTo(startFLIApril);
        assertThat(periodeMedSøkerInfoDtos.get(0).getPeriode().getTom()).isEqualTo(startSNIApril.minusDays(1));
        assertThat(periodeMedSøkerInfoDtos.get(0).getSøkerFrilansIPeriode()).isTrue();
        assertThat(periodeMedSøkerInfoDtos.get(0).getSøkerNæringIPeriode()).isFalse();

        assertThat(periodeMedSøkerInfoDtos.get(1).getPeriode().getFom()).isEqualTo(startSNIApril);
        assertThat(periodeMedSøkerInfoDtos.get(1).getPeriode().getTom()).isEqualTo(sluttenIApril);
        assertThat(periodeMedSøkerInfoDtos.get(1).getSøkerFrilansIPeriode()).isTrue();
        assertThat(periodeMedSøkerInfoDtos.get(1).getSøkerNæringIPeriode()).isTrue();

        assertThat(periodeMedSøkerInfoDtos.get(2).getPeriode().getFom()).isEqualTo(startFLIMai);
        assertThat(periodeMedSøkerInfoDtos.get(2).getPeriode().getTom()).isEqualTo(startSNIMai.minusDays(1));
        assertThat(periodeMedSøkerInfoDtos.get(2).getSøkerFrilansIPeriode()).isTrue();
        assertThat(periodeMedSøkerInfoDtos.get(2).getSøkerNæringIPeriode()).isFalse();

        assertThat(periodeMedSøkerInfoDtos.get(3).getPeriode().getFom()).isEqualTo(startSNIMai);
        assertThat(periodeMedSøkerInfoDtos.get(3).getPeriode().getTom()).isEqualTo(sluttenIMai);
        assertThat(periodeMedSøkerInfoDtos.get(3).getSøkerFrilansIPeriode()).isTrue();
        assertThat(periodeMedSøkerInfoDtos.get(3).getSøkerNæringIPeriode()).isTrue();
    }
}
