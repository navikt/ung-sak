package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static no.nav.ung.sak.domene.typer.tid.AbstractLocalDateInterval.TIDENES_ENDE;
import static no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.MaksdatoOpphørVarslingPeriode.VARSEL_UKER_FØR_MAKSDATO;
import static org.assertj.core.api.Assertions.assertThat;

class MaksdatoOpphørVarslingPeriodeTest {

    private static final LocalDate MAKSDATO_INNENFOR_VARSLINGSVINDU = LocalDate.now().plusWeeks(VARSEL_UKER_FØR_MAKSDATO);
    private static final LocalDate MAKSDATO_UTENFOR_VARSLINGSVINDU = MAKSDATO_INNENFOR_VARSLINGSVINDU.plusDays(1);

    @Test
    void skal_være_relevant_når_programperioden_er_åpen() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(TIDENES_ENDE, MAKSDATO_INNENFOR_VARSLINGSVINDU)).isTrue();
    }

    @Test
    void skal_være_relevant_når_programperioden_slutter_på_maksdato() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(MAKSDATO_INNENFOR_VARSLINGSVINDU, MAKSDATO_INNENFOR_VARSLINGSVINDU)).isTrue();
    }

    @Test
    void skal_være_relevant_når_programperioden_slutter_etter_maksdato() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(MAKSDATO_INNENFOR_VARSLINGSVINDU.plusDays(1), MAKSDATO_INNENFOR_VARSLINGSVINDU)).isTrue();
    }

    @Test
    void skal_ikke_være_relevant_når_programperioden_slutter_før_maksdato() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(MAKSDATO_INNENFOR_VARSLINGSVINDU.minusDays(1), MAKSDATO_INNENFOR_VARSLINGSVINDU)).isFalse();
    }

    @Test
    void skal_ikke_være_relevant_når_maksdato_er_utenfor_varslingsvinduet() {
        assertThat(MaksdatoOpphørVarslingPeriode.erRelevantForVarsling(TIDENES_ENDE, MAKSDATO_UTENFOR_VARSLINGSVINDU)).isFalse();
    }
}
