package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;

import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeGrunnlag;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.MaksdatoOpphørVarslingPeriode.VARSEL_UKER_FØR_MAKSDATO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaksdatoOpphørVarslingPeriodeTest {

    private static final LocalDate FOM = LocalDate.now().minusMonths(6);
    private static final LocalDate MAKSDATO_INNENFOR_VARSLINGSVINDU = LocalDate.now().plusWeeks(VARSEL_UKER_FØR_MAKSDATO);
    private static final LocalDate MAKSDATO_UTENFOR_VARSLINGSVINDU = MAKSDATO_INNENFOR_VARSLINGSVINDU.plusDays(1);

    @Test
    void skal_være_relevant_når_programperioden_er_åpen() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(lagGrunnlag(TIDENES_ENDE, MAKSDATO_INNENFOR_VARSLINGSVINDU))).isTrue();
    }

    @Test
    void skal_være_relevant_når_programperioden_slutter_på_maksdato() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(lagGrunnlag(MAKSDATO_INNENFOR_VARSLINGSVINDU, MAKSDATO_INNENFOR_VARSLINGSVINDU))).isTrue();
    }

    @Test
    void skal_være_relevant_når_programperioden_slutter_etter_maksdato() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(lagGrunnlag(MAKSDATO_INNENFOR_VARSLINGSVINDU.plusDays(1), MAKSDATO_INNENFOR_VARSLINGSVINDU))).isTrue();
    }

    @Test
    void skal_ikke_være_relevant_når_programperioden_slutter_før_maksdato() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(lagGrunnlag(MAKSDATO_INNENFOR_VARSLINGSVINDU.minusDays(1), MAKSDATO_INNENFOR_VARSLINGSVINDU))).isFalse();
    }

    @Test
    void skal_ikke_være_relevant_når_maksdato_er_utenfor_varslingsvinduet() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(lagGrunnlag(TIDENES_ENDE, MAKSDATO_UTENFOR_VARSLINGSVINDU))).isFalse();
    }

    @Test
    void skal_feile_når_maksdato_mangler() {
        assertThatThrownBy(() -> MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(lagGrunnlag(TIDENES_ENDE, null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("periodeMaksDato");
    }

    private static UngdomsprogramPeriodeGrunnlag lagGrunnlag(LocalDate tom, LocalDate maksdato) {
        var grunnlag = mock(UngdomsprogramPeriodeGrunnlag.class);
        when(grunnlag.hentForEksaktEnPeriode()).thenReturn(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, tom));
        when(grunnlag.getPeriodeMaksDato()).thenReturn(Optional.ofNullable(maksdato));
        return grunnlag;
    }
}
