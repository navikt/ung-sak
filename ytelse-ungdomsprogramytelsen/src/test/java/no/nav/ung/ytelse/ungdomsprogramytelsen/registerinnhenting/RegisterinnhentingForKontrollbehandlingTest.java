package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinnhenting;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.db.util.CdiDbAwareTest;
import no.nav.ung.sak.domene.registerinnhenting.task.InnhentIAYIAbakusTask;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@CdiDbAwareTest
class RegisterinnhentingForKontrollbehandlingTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Test
    void kontrollbehandling_skal_bare_hente_inntekt_og_ikke_personopplysninger_eller_programperiode() {
        var førstegangsbehandling = TestScenarioBuilder.builderMedSøknad().lagre(entityManager);
        var kontrollbehandling = Behandling.fraTidligereBehandling(førstegangsbehandling, BehandlingType.KONTROLL)
            .medBehandlingÅrsak(BehandlingÅrsak.builder(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT))
            .build();

        assertThat(behandlingProsesseringTjeneste.utledRegisterinnhentingTaskTyper(kontrollbehandling))
            .containsExactly(InnhentIAYIAbakusTask.TASKTYPE);
    }
}
