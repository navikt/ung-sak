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

    @Test
    void skal_slå_sammen_perioder_som_ligger_inntil_hverandre_og_har_samme_innhold() {
        LocalDate idag = LocalDate.now();
        LocalDate imorgen = idag.plusDays(1);
        Periode p1 = new Periode(idag, idag);
        Periode p2 = new Periode(imorgen, imorgen);
        FraværPeriode fp1 = new FraværPeriode(p1, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));
        FraværPeriode fp2 = new FraværPeriode(p2, Duration.ofHours(4), FraværÅrsak.ORDINÆRT_FRAVÆR, List.of(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET));

        List<FraværPeriode> resultat = FraværPeriodeSammenslåer.slåSammen(Arrays.asList(fp1, fp2));
        assertThat(resultat).hasSize(1);
        FraværPeriode sammenslåttPeriode = resultat.get(0);
        assertThat(sammenslåttPeriode.getPeriode()).isEqualTo(new Periode(idag, imorgen));
        assertThat(sammenslåttPeriode.getDuration()).isEqualTo(Duration.ofHours(4));
        assertThat(sammenslåttPeriode.getÅrsak()).isEqualTo(FraværÅrsak.ORDINÆRT_FRAVÆR);
        assertThat(sammenslåttPeriode.getAktivitetFravær()).containsOnly(AktivitetFravær.FRILANSER, AktivitetFravær.SELVSTENDIG_VIRKSOMHET);
    }
}
