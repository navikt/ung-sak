package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPerioder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste.EndretDato;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UngdomsprogramPeriodeTjenesteTest {

    private static final long BEHANDLING_ID = 1L;
    private static final LocalDate FOM = LocalDate.of(2026, 1, 5);
    private static final LocalDate MAKSDATO = LocalDate.of(2026, 10, 30);

    @Nested
    class FinnEndretSluttdatoer {

        @Test
        void skal_ikke_gi_endring_når_åpen_periode_lukkes_på_maksdato() {
            var resultat = finnEndretSluttdatoer(TIDENES_ENDE, MAKSDATO, MAKSDATO);

            assertThat(resultat).isEmpty();
        }

        @Test
        void skal_ikke_gi_endring_når_åpen_periode_lukkes_etter_maksdato() {
            var resultat = finnEndretSluttdatoer(TIDENES_ENDE, MAKSDATO.plusDays(10), MAKSDATO);

            assertThat(resultat).isEmpty();
        }

        @Test
        void skal_ikke_gi_endring_når_sluttdato_etter_maksdato_flyttes_til_maksdato() {
            var resultat = finnEndretSluttdatoer(MAKSDATO.plusDays(10), MAKSDATO, MAKSDATO);

            assertThat(resultat).isEmpty();
        }

        @Test
        void skal_gi_endring_når_åpen_periode_lukkes_før_maksdato() {
            var nySluttdato = MAKSDATO.minusDays(1);

            var resultat = finnEndretSluttdatoer(TIDENES_ENDE, nySluttdato, MAKSDATO);

            assertThat(resultat).containsExactly(new EndretDato(nySluttdato, TIDENES_ENDE));
        }

        @Test
        void skal_gi_endring_når_sluttdato_på_maksdato_flyttes_etter_maksdato() {
            // Forrige sluttdato var ikke etter maksdato, så endringen dekkes ikke av varsel om opphør ved maksdato
            var nySluttdato = MAKSDATO.plusDays(10);

            var resultat = finnEndretSluttdatoer(MAKSDATO, nySluttdato, MAKSDATO);

            assertThat(resultat).containsExactly(new EndretDato(nySluttdato, MAKSDATO));
        }

        @Test
        void skal_gi_endring_når_sluttdato_før_maksdato_gjenåpnes() {
            var forrigeSluttdato = MAKSDATO.minusDays(10);

            var resultat = finnEndretSluttdatoer(forrigeSluttdato, TIDENES_ENDE, MAKSDATO);

            assertThat(resultat).containsExactly(new EndretDato(TIDENES_ENDE, forrigeSluttdato));
        }

        @Test
        void skal_gi_endring_når_maksdato_mangler() {
            var nySluttdato = MAKSDATO.plusDays(10);

            var resultat = finnEndretSluttdatoer(TIDENES_ENDE, nySluttdato, null);

            assertThat(resultat).containsExactly(new EndretDato(nySluttdato, TIDENES_ENDE));
        }

        @Test
        void skal_ikke_gi_endring_når_sluttdato_er_uendret() {
            var resultat = finnEndretSluttdatoer(MAKSDATO.minusDays(1), MAKSDATO.minusDays(1), MAKSDATO);

            assertThat(resultat).isEmpty();
        }

        private static List<EndretDato> finnEndretSluttdatoer(LocalDate forrigeSluttdato, LocalDate nySluttdato, LocalDate maksdato) {
            var forrige = lagGrunnlag(maksdato, new UngdomsprogramPeriode(FOM, forrigeSluttdato));
            var ny = lagGrunnlag(maksdato, new UngdomsprogramPeriode(FOM, nySluttdato));
            return UngdomsprogramPeriodeTjeneste.finnEndretSluttdatoer(ny, forrige);
        }
    }

    @Nested
    class FinnPerioderKappetMotMaksdato {
        // Selve kappingen er testet i UngdomsprogramOpphørUtlederTest

        @Test
        void skal_returnere_tom_mengde_når_grunnlag_mangler() {
            var repository = mock(UngdomsprogramPeriodeRepository.class);
            when(repository.hentGrunnlag(BEHANDLING_ID)).thenReturn(Optional.empty());

            var resultat = new UngdomsprogramPeriodeTjeneste(repository).finnPerioderKappetMotMaksdato(BEHANDLING_ID);

            assertThat(resultat).isEmpty();
        }

        @Test
        void skal_kappe_periodene_i_behandlingens_grunnlag_mot_maksdato() {
            var repository = mock(UngdomsprogramPeriodeRepository.class);
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, TIDENES_ENDE));
            when(repository.hentGrunnlag(BEHANDLING_ID)).thenReturn(Optional.of(grunnlag));

            var resultat = new UngdomsprogramPeriodeTjeneste(repository).finnPerioderKappetMotMaksdato(BEHANDLING_ID);

            assertThat(resultat).containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, MAKSDATO));
        }
    }

    private static UngdomsprogramPeriodeGrunnlag lagGrunnlag(LocalDate maksdato, UngdomsprogramPeriode... perioder) {
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        var periodeSett = Arrays.stream(perioder).collect(Collectors.toSet());
        when(grunnlag.getUngdomsprogramPerioder()).thenReturn(new UngdomsprogramPerioder(periodeSett));
        when(grunnlag.getPeriodeMaksDato()).thenReturn(Optional.ofNullable(maksdato));
        if (perioder.length == 1) {
            var periode = perioder[0].getPeriode();
            when(grunnlag.hentForEksaktEnPeriodeDersomFinnes()).thenReturn(Optional.of(periode));
            when(grunnlag.hentForEksaktEnPeriode()).thenReturn(periode);
        }
        return grunnlag;
    }
}
