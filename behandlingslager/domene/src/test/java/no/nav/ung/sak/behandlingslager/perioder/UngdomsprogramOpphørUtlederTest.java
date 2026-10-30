package no.nav.ung.sak.behandlingslager.perioder;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static no.nav.k9.felles.konfigurasjon.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UngdomsprogramOpphørUtlederTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 5);
    private static final LocalDate MAKSDATO = LocalDate.of(2026, 10, 30);

    @Nested
    class FinnSisteDagMedYtelse {

        @Test
        void skal_gi_maksdato_når_programperioden_er_åpen() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, TIDENES_ENDE));

            assertThat(UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(grunnlag)).isEqualTo(MAKSDATO);
        }

        @Test
        void skal_gi_maksdato_når_programperioden_slutter_etter_maksdato() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, MAKSDATO.plusDays(1)));

            assertThat(UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(grunnlag)).isEqualTo(MAKSDATO);
        }

        @Test
        void skal_gi_maksdato_når_programperioden_slutter_på_maksdato() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, MAKSDATO));

            assertThat(UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(grunnlag)).isEqualTo(MAKSDATO);
        }

        @Test
        void skal_gi_programperiodens_sluttdato_når_den_er_før_maksdato() {
            var tom = MAKSDATO.minusDays(1);
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, tom));

            assertThat(UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(grunnlag)).isEqualTo(tom);
        }

        @Test
        void skal_feile_når_maksdato_mangler() {
            var grunnlag = lagGrunnlag(null, new UngdomsprogramPeriode(FOM, TIDENES_ENDE));

            assertThatThrownBy(() -> UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(grunnlag))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("periodeMaksDato");
        }

        @Test
        void skal_feile_når_grunnlaget_har_flere_perioder() {
            var grunnlag = lagGrunnlag(MAKSDATO,
                new UngdomsprogramPeriode(FOM, FOM.plusDays(10)),
                new UngdomsprogramPeriode(FOM.plusDays(20), TIDENES_ENDE));

            assertThatThrownBy(() -> UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(grunnlag))
                .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class OpphørerVedMaksdato {

        @Test
        void skal_opphøre_ved_maksdato_når_programperioden_er_åpen() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, TIDENES_ENDE));

            assertThat(UngdomsprogramOpphørUtleder.opphørerVedMaksdato(grunnlag)).isTrue();
        }

        @Test
        void skal_opphøre_ved_maksdato_når_programperioden_slutter_etter_maksdato() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, MAKSDATO.plusDays(1)));

            assertThat(UngdomsprogramOpphørUtleder.opphørerVedMaksdato(grunnlag)).isTrue();
        }

        @Test
        void skal_opphøre_ved_maksdato_når_programperioden_slutter_på_maksdato() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, MAKSDATO));

            assertThat(UngdomsprogramOpphørUtleder.opphørerVedMaksdato(grunnlag)).isTrue();
        }

        @Test
        void skal_ikke_opphøre_ved_maksdato_når_programperioden_slutter_før_maksdato() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, MAKSDATO.minusDays(1)));

            assertThat(UngdomsprogramOpphørUtleder.opphørerVedMaksdato(grunnlag)).isFalse();
        }

        @Test
        void skal_feile_når_maksdato_mangler() {
            var grunnlag = lagGrunnlag(null, new UngdomsprogramPeriode(FOM, TIDENES_ENDE));

            assertThatThrownBy(() -> UngdomsprogramOpphørUtleder.opphørerVedMaksdato(grunnlag))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("periodeMaksDato");
        }
    }

    @Nested
    class FinnPerioderKappetMotMaksdato {

        @Test
        void skal_beholde_perioder_uendret_når_maksdato_mangler() {
            var grunnlag = lagGrunnlag(null, new UngdomsprogramPeriode(FOM, TIDENES_ENDE));

            assertThat(UngdomsprogramOpphørUtleder.finnPerioderKappetMotMaksdato(grunnlag))
                .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, TIDENES_ENDE));
        }

        @Test
        void skal_kappe_åpen_periode_på_maksdato() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, TIDENES_ENDE));

            assertThat(UngdomsprogramOpphørUtleder.finnPerioderKappetMotMaksdato(grunnlag))
                .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, MAKSDATO));
        }

        @Test
        void skal_kappe_periode_som_slutter_etter_maksdato() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, MAKSDATO.plusDays(1)));

            assertThat(UngdomsprogramOpphørUtleder.finnPerioderKappetMotMaksdato(grunnlag))
                .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, MAKSDATO));
        }

        @Test
        void skal_beholde_periode_som_slutter_på_maksdato() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, MAKSDATO));

            assertThat(UngdomsprogramOpphørUtleder.finnPerioderKappetMotMaksdato(grunnlag))
                .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, MAKSDATO));
        }

        @Test
        void skal_beholde_periode_som_slutter_før_maksdato() {
            var tom = MAKSDATO.minusDays(1);
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(FOM, tom));

            assertThat(UngdomsprogramOpphørUtleder.finnPerioderKappetMotMaksdato(grunnlag))
                .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, tom));
        }

        @Test
        void skal_beholde_periode_som_starter_på_maksdato_som_én_dag() {
            var grunnlag = lagGrunnlag(MAKSDATO, new UngdomsprogramPeriode(MAKSDATO, TIDENES_ENDE));

            assertThat(UngdomsprogramOpphørUtleder.finnPerioderKappetMotMaksdato(grunnlag))
                .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(MAKSDATO, MAKSDATO));
        }

        @Test
        void skal_fjerne_periode_som_starter_etter_maksdato() {
            var grunnlag = lagGrunnlag(MAKSDATO,
                new UngdomsprogramPeriode(FOM, MAKSDATO.minusDays(10)),
                new UngdomsprogramPeriode(MAKSDATO.plusDays(1), TIDENES_ENDE));

            assertThat(UngdomsprogramOpphørUtleder.finnPerioderKappetMotMaksdato(grunnlag))
                .containsExactly(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, MAKSDATO.minusDays(10)));
        }
    }

    @Nested
    class ErEtterMaksdato {

        @Test
        void skal_være_etter_maksdato_dagen_etter_maksdato() {
            assertThat(UngdomsprogramOpphørUtleder.erEtterMaksdato(MAKSDATO.plusDays(1), MAKSDATO)).isTrue();
        }

        @Test
        void skal_ikke_være_etter_maksdato_på_maksdato() {
            assertThat(UngdomsprogramOpphørUtleder.erEtterMaksdato(MAKSDATO, MAKSDATO)).isFalse();
        }

        @Test
        void skal_ikke_være_etter_maksdato_før_maksdato() {
            assertThat(UngdomsprogramOpphørUtleder.erEtterMaksdato(MAKSDATO.minusDays(1), MAKSDATO)).isFalse();
        }
    }

    @Nested
    class LukketEllerÅpenSluttdato {

        @Test
        void skal_finne_lukket_sluttdato() {
            var grunnlag = lagGrunnlag(null, new UngdomsprogramPeriode(FOM, MAKSDATO));

            assertThat(UngdomsprogramOpphørUtleder.finnLukketSluttdato(grunnlag)).contains(MAKSDATO);
            assertThat(UngdomsprogramOpphørUtleder.harLukketSluttdato(grunnlag)).isTrue();
            assertThat(UngdomsprogramOpphørUtleder.harÅpenSluttdato(grunnlag)).isFalse();
        }

        @Test
        void skal_ikke_finne_lukket_sluttdato_når_programperioden_er_åpen() {
            var grunnlag = lagGrunnlag(null, new UngdomsprogramPeriode(FOM, TIDENES_ENDE));

            assertThat(UngdomsprogramOpphørUtleder.finnLukketSluttdato(grunnlag)).isEmpty();
            assertThat(UngdomsprogramOpphørUtleder.harLukketSluttdato(grunnlag)).isFalse();
            assertThat(UngdomsprogramOpphørUtleder.harÅpenSluttdato(grunnlag)).isTrue();
        }

        @Test
        void skal_verken_være_lukket_eller_åpen_uten_perioder() {
            var grunnlag = new UngdomsprogramPeriodeGrunnlag(1L);

            assertThat(UngdomsprogramOpphørUtleder.finnLukketSluttdato(grunnlag)).isEmpty();
            assertThat(UngdomsprogramOpphørUtleder.harLukketSluttdato(grunnlag)).isFalse();
            assertThat(UngdomsprogramOpphørUtleder.harÅpenSluttdato(grunnlag)).isFalse();
        }
    }

    private static UngdomsprogramPeriodeGrunnlag lagGrunnlag(LocalDate maksdato, UngdomsprogramPeriode... perioder) {
        var grunnlag = new UngdomsprogramPeriodeGrunnlag(1L);
        grunnlag.leggTil(List.of(perioder));
        if (maksdato != null) {
            grunnlag.setUngdomsprogramMaksPeriode(new UngdomsprogramMaksPeriode(false, maksdato));
        }
        return grunnlag;
    }
}
