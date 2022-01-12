package no.nav.k9.sak.behandling.revurdering;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

class OpprettRevurderingEllerOpprettDiffTaskTest {

    private OpprettRevurderingEllerOpprettDiffTask task = new OpprettRevurderingEllerOpprettDiffTask();

    @Test
    void skal_parse_korrekt_ved_et_element() {
        var perioder = task.parseToPeriodeSet(LocalDate.now() + "/" + LocalDate.now().plusDays(14));

        assertThat(perioder).hasSize(1);
        assertThat(perioder).contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(14)));
    }

    @Test
    void skal_parse_korrekt_ved_flere_element() {
        var perioder = task.parseToPeriodeSet(LocalDate.now() + "/" + LocalDate.now().plusDays(14) + "|" + LocalDate.now().plusDays(50) + "/" + LocalDate.now().plusDays(90));

        assertThat(perioder).hasSize(2);
        assertThat(perioder).contains(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(14)),
            DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().plusDays(50), LocalDate.now().plusDays(90)));
    }
}
