package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

import static no.nav.k9.sak.ytelse.omsorgspenger.mottak.FraværPeriodeSammenslåer.fjernHelgdager;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.k9.søknad.felles.fravær.AktivitetFravær;
import no.nav.k9.søknad.felles.fravær.FraværPeriode;
import no.nav.k9.søknad.felles.fravær.FraværÅrsak;
import no.nav.k9.søknad.felles.type.Periode;

class FraværPeriodeSammenslåerTest {
    LocalDate mandag = LocalDate.of(2021, 4, 26);

    Periode mån = new Periode(mandag, mandag);
    Periode tis = new Periode(mandag.plusDays(1), mandag.plusDays(1));
    Periode ons = new Periode(mandag.plusDays(2), mandag.plusDays(2));
    Periode lør = new Periode(mandag.plusDays(5), mandag.plusDays(5));

    @Test
    void skal_slå_sammen_perioder_som_ligger_inntil_hverandre_og_har_samme_innhold() {
        FraværPeriode fp1 = new FraværPeriode(mån, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp2 = new FraværPeriode(tis, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp3 = new FraværPeriode(ons, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2, fp3));
        assertThat(resultat).hasSize(1);
        FraværPeriode sammenslåttPeriode = resultat.get(0);
        assertThat(sammenslåttPeriode.getPeriode()).isEqualTo(new Periode(mandag, mandag.plusDays(2)));
        assertThat(sammenslåttPeriode.getDuration()).isEqualTo(Duration.ofHours(4));
        assertThat(sammenslåttPeriode.getÅrsak()).isEqualTo(FraværÅrsak.ORDINÆRT_FRAVÆR);
        assertThat(sammenslåttPeriode.getAktivitetFravær()).containsOnly(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET);
    }

    @Test
    void skal_ikke_slå_sammen_perioder_som_ikke_ligger_inntil_hverandre() {
        FraværPeriode fp1 = new FraværPeriode(mån, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp2 = new FraværPeriode(ons, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2));
        assertThat(resultat).containsOnly(fp1, fp2);
    }

    @Test
    void skal_ikke_slå_sammen_perioder_som_har_ulik_duration() {
        FraværPeriode fp1 = new FraværPeriode(mån, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp2 = new FraværPeriode(tis, Duration.ofHours(1), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2));
        assertThat(resultat).containsOnly(fp1, fp2);
    }

    @Test
    void skal_ikke_slå_sammen_perioder_som_har_ulik_årsak() {
        FraværPeriode fp1 = new FraværPeriode(mån, Duration.ofHours(4), FraværÅrsak.SMITTEVERNHENSYN, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp2 = new FraværPeriode(tis, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2));
        assertThat(resultat).containsOnly(fp1, fp2);
    }

    @Test
    void skal_ikke_slå_sammen_perioder_som_har_ulik_aktivitet() {
        FraværPeriode fp1 = new FraværPeriode(mån, Duration.ofHours(4), FraværÅrsak.SMITTEVERNHENSYN, List.of(AktivitetFravær.FRILANSER));
        FraværPeriode fp2 = new FraværPeriode(tis, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2));
        assertThat(resultat).containsOnly(fp1, fp2);
    }

    @Test
    void skal_ikke_slå_sammen_helgdager_med_arbeidsdager() {
        FraværPeriode fp1 = new FraværPeriode(mån, Duration.ofHours(4), FraværÅrsak.SMITTEVERNHENSYN, List.of(AktivitetFravær.FRILANSER));
        FraværPeriode fp2 = new FraværPeriode(tis, Duration.ofHours(1), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp3 = new FraværPeriode(lør, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2, fp3));
        assertThat(resultat).containsOnly(fp1, fp2);
    }

    @Test
    void fjerner_helger_fra_perioder() {
        LocalDate fredag = mandag.plusDays(4);
        LocalDate lørdag = mandag.plusDays(5);
        LocalDate måndagUke2 = mandag.plusWeeks(1);
        LocalDate tirsdagUke2 = mandag.plusWeeks(1).plusDays(1);
        LocalDate fredagUke2 = mandag.plusWeeks(1).plusDays(4);
        LocalDate måndagUke3 = mandag.plusWeeks(2);
        LocalDate tirsdagUke3 = mandag.plusWeeks(2).plusDays(1);

        assertThat(fjernHelgdager(new Periode(lørdag, lørdag))).isEmpty();
        assertThat(fjernHelgdager(new Periode(mandag, mandag))).containsOnly(new Periode(mandag, mandag));
        assertThat(fjernHelgdager(new Periode(mandag, fredag))).containsOnly(new Periode(mandag, fredag));
        assertThat(fjernHelgdager(new Periode(mandag, lørdag))).containsOnly(new Periode(mandag, fredag));
        assertThat(fjernHelgdager(new Periode(lørdag, måndagUke2))).containsOnly(new Periode(måndagUke2, måndagUke2));
        assertThat(fjernHelgdager(new Periode(fredag, måndagUke2))).containsOnly(new Periode(fredag, fredag), new Periode(måndagUke2, måndagUke2));
        assertThat(fjernHelgdager(new Periode(mandag, tirsdagUke2))).containsOnly(new Periode(mandag, fredag), new Periode(måndagUke2, tirsdagUke2));
        assertThat(fjernHelgdager(new Periode(mandag, tirsdagUke3))).containsOnly(new Periode(mandag, fredag), new Periode(måndagUke2, fredagUke2), new Periode(måndagUke3, tirsdagUke3));
    }
}
