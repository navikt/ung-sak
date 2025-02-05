package no.nav.ung.sak.domene.behandling.steg.uttak;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.kodeverk.ungdomsytelse.uttak.UngdomsytelseUttakAvslagsårsak;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.domene.behandling.steg.uttak.regler.UttakResultat;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class UttaksperiodeMapperTest {

    @Test
    void skal_prioritere_søkers_dødsfall_avslagsårsak() {
        final var fom = LocalDate.now();
        final var tom = LocalDate.now();
        final var resultat = UttaksperiodeMapper.mapTilUttaksperioder(List.of(
            new LocalDateTimeline<>(fom, tom, UttakResultat.forAvslag(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL)),
            new LocalDateTimeline<>(fom, tom, UttakResultat.forAvslag(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER))));

        assertThat(resultat.size()).isEqualTo(1);
        assertThat(resultat.get(0).getAvslagsårsak()).isEqualTo(UngdomsytelseUttakAvslagsårsak.SØKERS_DØDSFALL);
        assertThat(resultat.get(0).getUtbetalingsgrad().compareTo(BigDecimal.ZERO)).isEqualTo(0);
    }


    @Test
    void skal_avslå_dersom_en_del_er_avslått() {
        final var fom1 = LocalDate.now();
        final var tom1 = LocalDate.now().plusDays(2);
        final var fom2 = LocalDate.now().plusDays(2);
        final var tom2 = LocalDate.now().plusDays(3);

        final var resultat = UttaksperiodeMapper.mapTilUttaksperioder(List.of(
            new LocalDateTimeline<>(fom1, tom1, UttakResultat.forInnvilgelse(BigDecimal.valueOf(50))),
            new LocalDateTimeline<>(fom2, tom2, UttakResultat.forAvslag(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER))));

        assertThat(resultat.size()).isEqualTo(2);
        assertThat(resultat.get(0).getAvslagsårsak()).isNull();
        assertThat(resultat.get(0).getUtbetalingsgrad().compareTo(BigDecimal.valueOf(50))).isEqualTo(0);
        assertThat(resultat.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom1, fom2.minusDays(1)));

        assertThat(resultat.get(1).getAvslagsårsak()).isEqualTo(UngdomsytelseUttakAvslagsårsak.IKKE_NOK_DAGER);
        assertThat(resultat.get(1).getUtbetalingsgrad().compareTo(BigDecimal.ZERO)).isEqualTo(0);
        assertThat(resultat.get(1).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2));
    }

    @Test
    void skal_kaste_feil_dersom_overlappende_perioder_innvilget() {
        final var fom = LocalDate.now();
        final var tom = LocalDate.now();
        final var input = List.of(
            new LocalDateTimeline<>(fom, tom, UttakResultat.forInnvilgelse(BigDecimal.ZERO)),
            new LocalDateTimeline<>(fom, tom, UttakResultat.forInnvilgelse(BigDecimal.TEN)));
        assertThrows(IllegalStateException.class, () ->
            UttaksperiodeMapper.mapTilUttaksperioder(input));
    }

}
