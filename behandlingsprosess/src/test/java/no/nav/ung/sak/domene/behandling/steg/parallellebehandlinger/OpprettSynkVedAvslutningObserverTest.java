package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_KOMPLETTHET;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.KontrollbehandlingBryter;
import no.nav.ung.sak.behandlingslager.behandling.kontroll.ParallelleBehandlingerRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class OpprettSynkVedAvslutningObserverTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private SynkroniserÅpneBehandlingerTaskOppretter taskOppretter;
    private Behandling førstegangsbehandling;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        taskOppretter = mock(SynkroniserÅpneBehandlingerTaskOppretter.class);
        førstegangsbehandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
    }

    @Test
    void oppretter_ikke_synk_uten_andre_åpne_behandlinger() {
        observer(true).vedAvslutning(avsluttetEvent(førstegangsbehandling));

        verify(taskOppretter, never()).opprett(anyLong());
    }

    @Test
    void oppretter_synk_når_bryteren_er_på_og_fagsaken_har_annen_åpen_behandling() {
        nyBehandling(BehandlingType.REVURDERING);

        observer(true).vedAvslutning(avsluttetEvent(førstegangsbehandling));

        verify(taskOppretter).opprett(førstegangsbehandling.getFagsakId());
    }

    @Test
    void oppretter_synk_når_bryteren_er_av_og_fagsaken_har_åpen_kontrollbehandling() {
        nyBehandling(BehandlingType.KONTROLL);

        observer(false).vedAvslutning(avsluttetEvent(førstegangsbehandling));

        verify(taskOppretter).opprett(førstegangsbehandling.getFagsakId());
    }

    @Test
    void oppretter_ikke_synk_når_bryteren_er_av_og_fagsaken_ikke_har_åpen_kontrollbehandling() {
        nyBehandling(BehandlingType.REVURDERING);

        observer(false).vedAvslutning(avsluttetEvent(førstegangsbehandling));

        verify(taskOppretter, never()).opprett(anyLong());
    }

    @Test
    void oppretter_ikke_synk_når_avsluttet_behandling_ikke_er_ytelsesbehandling() {
        nyBehandling(BehandlingType.KONTROLL);
        var klage = nyBehandling(BehandlingType.KLAGE);

        observer(true).vedAvslutning(avsluttetEvent(klage));

        verify(taskOppretter, never()).opprett(anyLong());
    }

    private OpprettSynkVedAvslutningObserver observer(boolean bryterPå) {
        return new OpprettSynkVedAvslutningObserver(behandlingRepository, new ParallelleBehandlingerRepository(entityManager), taskOppretter,
            new KontrollbehandlingBryter(bryterPå, bryterPå));
    }

    private BehandlingStatusEvent.BehandlingAvsluttetEvent avsluttetEvent(Behandling behandling) {
        var kontekst = new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling));
        return BehandlingStatusEvent.nyEvent(kontekst, BehandlingStatus.AVSLUTTET, BehandlingStatus.IVERKSETTER_VEDTAK);
    }

    private Behandling nyBehandling(BehandlingType type) {
        var behandling = Behandling.fraTidligereBehandling(førstegangsbehandling, type).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        if (type.erYtelseBehandlingType()) {
            new InternalManipulerBehandling().forceOppdaterBehandlingSteg(behandling, VURDER_KOMPLETTHET);
            behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        }
        entityManager.flush();
        return behandling;
    }
}
