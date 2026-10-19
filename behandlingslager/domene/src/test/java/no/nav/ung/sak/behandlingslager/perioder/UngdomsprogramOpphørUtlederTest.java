package no.nav.ung.sak.behandlingslager.perioder;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static no.nav.k9.felles.konfigurasjon.konfig.Tid.TIDENES_ENDE;
import static org.assertj.core.api.Assertions.assertThat;

class UngdomsprogramOpphørUtlederTest {

    private static final LocalDate FOM = LocalDate.of(2026, 1, 5);
    private static final LocalDate MAKSDATO = LocalDate.of(2026, 10, 30);

    @Nested
    class FinnSisteDagMedYtelse {

        @Test
        void skal_gi_maksdato_når_programperioden_er_åpen() {
            assertThat(UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(TIDENES_ENDE, MAKSDATO)).isEqualTo(MAKSDATO);
        }

        @Test
        void skal_gi_maksdato_når_programperioden_slutter_etter_maksdato() {
            assertThat(UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(MAKSDATO.plusDays(1), MAKSDATO)).isEqualTo(MAKSDATO);
        }

        @Test
        void skal_gi_maksdato_når_programperioden_slutter_på_maksdato() {
            assertThat(UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(MAKSDATO, MAKSDATO)).isEqualTo(MAKSDATO);
        }

        @Test
        void skal_gi_programperiodens_sluttdato_når_den_er_før_maksdato() {
            var tom = MAKSDATO.minusDays(1);

            assertThat(UngdomsprogramOpphørUtleder.finnSisteDagMedYtelse(tom, MAKSDATO)).isEqualTo(tom);
        }
    }

    @Nested
    class OpphørerVedMaksdato {

        @Test
        void skal_opphøre_ved_maksdato_når_programperioden_er_åpen() {
            assertThat(UngdomsprogramOpphørUtleder.opphørerVedMaksdato(TIDENES_ENDE, MAKSDATO)).isTrue();
        }

        @Test
        void skal_opphøre_ved_maksdato_når_programperioden_slutter_etter_maksdato() {
            assertThat(UngdomsprogramOpphørUtleder.opphørerVedMaksdato(MAKSDATO.plusDays(1), MAKSDATO)).isTrue();
        }

        @Test
        void skal_opphøre_ved_maksdato_når_programperioden_slutter_på_maksdato() {
            assertThat(UngdomsprogramOpphørUtleder.opphørerVedMaksdato(MAKSDATO, MAKSDATO)).isTrue();
        }

        @Test
        void skal_ikke_opphøre_ved_maksdato_når_programperioden_slutter_før_maksdato() {
            assertThat(UngdomsprogramOpphørUtleder.opphørerVedMaksdato(MAKSDATO.minusDays(1), MAKSDATO)).isFalse();
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
            var grunnlag = lagGrunnlag(new UngdomsprogramPeriode(FOM, MAKSDATO));

            assertThat(UngdomsprogramOpphørUtleder.finnLukketSluttdato(grunnlag)).contains(MAKSDATO);
            assertThat(UngdomsprogramOpphørUtleder.harLukketSluttdato(grunnlag)).isTrue();
            assertThat(UngdomsprogramOpphørUtleder.harÅpenSluttdato(grunnlag)).isFalse();
        }

        @Test
        void skal_ikke_finne_lukket_sluttdato_når_programperioden_er_åpen() {
            var grunnlag = lagGrunnlag(new UngdomsprogramPeriode(FOM, TIDENES_ENDE));

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

    private static UngdomsprogramPeriodeGrunnlag lagGrunnlag(UngdomsprogramPeriode periode) {
        var grunnlag = new UngdomsprogramPeriodeGrunnlag(1L);
        grunnlag.leggTil(List.of(periode));
        return grunnlag;
    }
}
