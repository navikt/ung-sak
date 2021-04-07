package no.nav.k9.sak.ytelse.omsorgspenger.mottak;

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
    LocalDate nå = LocalDate.now();

    Periode iDag = new Periode(nå, nå);
    Periode iMorgen = new Periode(nå.plusDays(1), nå.plusDays(1));
    Periode iOverimorgen = new Periode(nå.plusDays(2), nå.plusDays(2));

    @Test
    void skal_slå_sammen_perioder_som_ligger_inntil_hverandre_og_har_samme_innhold() {
        FraværPeriode fp1 = new FraværPeriode(iDag, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp2 = new FraværPeriode(iMorgen, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp3 = new FraværPeriode(iOverimorgen, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2, fp3));
        assertThat(resultat).hasSize(1);
        FraværPeriode sammenslåttPeriode = resultat.get(0);
        assertThat(sammenslåttPeriode.getPeriode()).isEqualTo(new Periode(nå, nå.plusDays(2)));
        assertThat(sammenslåttPeriode.getDuration()).isEqualTo(Duration.ofHours(4));
        assertThat(sammenslåttPeriode.getÅrsak()).isEqualTo(FraværÅrsak.ORDINÆRT_FRAVÆR);
        assertThat(sammenslåttPeriode.getAktivitetFravær()).containsOnly(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET);
    }

    @Test
    void skal_ikke_slå_sammen_perioder_som_ikke_ligger_inntil_hverandre() {
        FraværPeriode fp1 = new FraværPeriode(iDag, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp2 = new FraværPeriode(iOverimorgen, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2));
        assertThat(resultat).containsOnly(fp1, fp2);
    }

    @Test
    void skal_ikke_slå_sammen_perioder_som_har_ulik_duration() {
        FraværPeriode fp1 = new FraværPeriode(iDag, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp2 = new FraværPeriode(iMorgen, Duration.ofHours(1), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2));
        assertThat(resultat).containsOnly(fp1, fp2);
    }

    @Test
    void skal_ikke_slå_sammen_perioder_som_har_ulik_årsak() {
        FraværPeriode fp1 = new FraværPeriode(iDag, Duration.ofHours(4), FraværÅrsak.SMITTEVERNHENSYN, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp2 = new FraværPeriode(iMorgen, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2));
        assertThat(resultat).containsOnly(fp1, fp2);
    }

    @Test
    void skal_ikke_slå_sammen_perioder_som_har_ulik_aktivitet() {
        FraværPeriode fp1 = new FraværPeriode(iDag, Duration.ofHours(4), FraværÅrsak.SMITTEVERNHENSYN, List.of(AktivitetFravær.FRILANSER));
        FraværPeriode fp2 = new FraværPeriode(iMorgen, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2));
        assertThat(resultat).containsOnly(fp1, fp2);
    }
}
