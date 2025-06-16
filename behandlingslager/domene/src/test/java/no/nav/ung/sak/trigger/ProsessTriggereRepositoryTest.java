package no.nav.ung.sak.trigger;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;

class ProsessTriggereRepositoryTest {

    private ProsessTriggereRepository repository = new ProsessTriggereRepository(null);

    @Test
    void skal_kunne_diffe_grunnlag() {
        var før = new ProsessTriggere(new Triggere(Set.of()));
        var etter = new ProsessTriggere(new Triggere(Set.of(new Trigger(BehandlingÅrsakType.NY_SØKT_PROGRAM_PERIODE, DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now().plusDays(2))))));
        var diff = repository.diff(false, før, etter);

        assertThat(diff.areDifferent()).isTrue();
    }
}
