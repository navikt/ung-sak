package no.nav.k9.sak.ytelse.frisinn.beregnytelse;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.uttak.UttakArbeidType;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitet;
import no.nav.k9.sak.domene.uttak.repo.UttakAktivitetPeriode;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultatPeriode;

public class MapUttakFrisinnTilRegelTest {
    private static final LocalDate STP = LocalDate.of(2020,4,1);

    @Test
    public void skal_teste_uttak_mapping_med_en_periode() {
        UttakAktivitetPeriode periode = new UttakAktivitetPeriode(STP, STP.plusMonths(1), UttakArbeidType.FRILANSER, null, null);
        UttakAktivitet aktivitet = new UttakAktivitet(periode);

        UttakResultat resultat = MapUttakFrisinnTilRegel.map(aktivitet, FagsakYtelseType.FRISINN);

        assertThat(resultat).isNotNull();
        LocalDateTimeline<UttakResultatPeriode> tidslinje = resultat.getUttakPeriodeTimeline();
        assertThat(tidslinje).isNotNull();
    }

    @Test
    public void skal_teste_uttak_mapping_med_to_perioder_uten_overlapp() {
        LocalDateInterval førsteInterval = new LocalDateInterval(STP, STP.plusDays(15));
        LocalDateInterval andreInterval = new LocalDateInterval(STP.plusDays(16), STP.plusDays(30));

        UttakAktivitetPeriode periode = new UttakAktivitetPeriode(førsteInterval.getFomDato(), førsteInterval.getTomDato(), UttakArbeidType.FRILANSER, null, null);
        UttakAktivitetPeriode periode2 = new UttakAktivitetPeriode(andreInterval.getFomDato(), andreInterval.getTomDato(), UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null);
        UttakAktivitet aktivitet = new UttakAktivitet(periode, periode2);

        UttakResultat resultat = MapUttakFrisinnTilRegel.map(aktivitet, FagsakYtelseType.FRISINN);

        assertThat(resultat).isNotNull();
        LocalDateTimeline<UttakResultatPeriode> tidslinje = resultat.getUttakPeriodeTimeline();
        assertThat(tidslinje).isNotNull();
        assertThat(tidslinje.size()).isEqualTo(2);
        assertThat(tidslinje.getSegment(førsteInterval).getValue().getUttakAktiviteter()).hasSize(1);
        assertThat(tidslinje.getSegment(førsteInterval).getValue().getUttakAktiviteter().stream().anyMatch(ut -> ut.getType().equals(UttakArbeidType.FRILANSER))).isTrue();
        assertThat(tidslinje.getSegment(andreInterval).getValue().getUttakAktiviteter()).hasSize(1);
        assertThat(tidslinje.getSegment(andreInterval).getValue().getUttakAktiviteter().stream().anyMatch(ut -> ut.getType().equals(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE))).isTrue();
    }

    @Test
    public void skal_teste_uttak_mapping_med_to_perioder_med_fullstendig_overlapp() {
        LocalDate tom = STP.plusMonths(1);
        UttakAktivitetPeriode periode = new UttakAktivitetPeriode(STP, tom, UttakArbeidType.FRILANSER, null, null);
        UttakAktivitetPeriode periode2 = new UttakAktivitetPeriode(STP, tom, UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null);
        UttakAktivitet aktivitet = new UttakAktivitet(periode, periode2);

        UttakResultat resultat = MapUttakFrisinnTilRegel.map(aktivitet, FagsakYtelseType.FRISINN);

        assertThat(resultat).isNotNull();
        LocalDateTimeline<UttakResultatPeriode> tidslinje = resultat.getUttakPeriodeTimeline();
        assertThat(tidslinje).isNotNull();
        assertThat(tidslinje.size()).isEqualTo(1);
        LocalDateInterval interval = new LocalDateInterval(STP, tom);
        LocalDateSegment<UttakResultatPeriode> segment = tidslinje.getSegment(interval);
        assertThat(segment).isNotNull();
        List<no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet> aktiviteter = segment.getValue().getUttakAktiviteter();
        assertThat(aktiviteter.stream().anyMatch(akt -> akt.getType().equals(UttakArbeidType.FRILANSER))).isTrue();
        assertThat(aktiviteter.stream().anyMatch(akt -> akt.getType().equals(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE))).isTrue();
    }

    @Test
    public void skal_teste_uttak_mapping_med_to_perioder_med_delvis_overlapp() {
        LocalDateInterval førsteInterval = new LocalDateInterval(STP, STP.plusDays(15));
        LocalDateInterval andreInterval = new LocalDateInterval(STP.plusDays(10), STP.plusDays(30));
        UttakAktivitetPeriode periode = new UttakAktivitetPeriode(førsteInterval.getFomDato(), førsteInterval.getTomDato(), UttakArbeidType.FRILANSER, null, null);
        UttakAktivitetPeriode periode2 = new UttakAktivitetPeriode(andreInterval.getFomDato(), andreInterval.getTomDato(), UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE, null, null);
        UttakAktivitet aktivitet = new UttakAktivitet(periode, periode2);

        UttakResultat resultat = MapUttakFrisinnTilRegel.map(aktivitet, FagsakYtelseType.FRISINN);

        assertThat(resultat).isNotNull();
        LocalDateTimeline<UttakResultatPeriode> tidslinje = resultat.getUttakPeriodeTimeline();
        assertThat(tidslinje).isNotNull();
        assertThat(tidslinje.size()).isEqualTo(3);

        LocalDateSegment<UttakResultatPeriode> førsteSegment = tidslinje.getSegment(førsteInterval);
        assertThat(førsteSegment).isNotNull();
        List<no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet> førsteAktiviteter = førsteSegment.getValue().getUttakAktiviteter();
        assertThat(førsteAktiviteter).hasSize(1);
        assertThat(førsteAktiviteter.stream().anyMatch(akt -> akt.getType().equals(UttakArbeidType.FRILANSER))).isTrue();

        LocalDateInterval intervalUnderOverlapp = new LocalDateInterval(STP.plusDays(10), STP.plusDays(15));
        LocalDateSegment<UttakResultatPeriode> andreSegment = tidslinje.getSegment(intervalUnderOverlapp);
        assertThat(andreSegment).isNotNull();
        List<no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet> andreAktiviteter = andreSegment.getValue().getUttakAktiviteter();
        assertThat(andreAktiviteter).hasSize(2);
        assertThat(andreAktiviteter.stream().anyMatch(akt -> akt.getType().equals(UttakArbeidType.FRILANSER))).isTrue();
        assertThat(andreAktiviteter.stream().anyMatch(akt -> akt.getType().equals(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE))).isTrue();

        LocalDateInterval intervalEtterOverlapp = new LocalDateInterval(STP.plusDays(16), STP.plusDays(30));
        LocalDateSegment<UttakResultatPeriode> tredjeSegment = tidslinje.getSegment(intervalEtterOverlapp);
        assertThat(tredjeSegment).isNotNull();
        List<no.nav.k9.sak.ytelse.beregning.regelmodell.UttakAktivitet> tredjeAktiviteter = tredjeSegment.getValue().getUttakAktiviteter();
        assertThat(tredjeAktiviteter).hasSize(1);
        assertThat(tredjeAktiviteter.stream().anyMatch(akt -> akt.getType().equals(UttakArbeidType.SELVSTENDIG_NÆRINGSDRIVENDE))).isTrue();
    }

}
