package no.nav.ung.sak.domene.behandling.steg.parallellebehandlinger;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_KOMPLETTHET;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingStatus;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.InternalManipulerBehandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.behandling.steg.iverksettevedtak.HenleggBehandlingTjeneste;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class HenleggKontrollbehandlingUtenMånederTaskTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepository behandlingRepository;
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;
    private HenleggBehandlingTjeneste henleggBehandlingTjeneste;
    private HenleggKontrollbehandlingUtenMånederTask task;
    private Behandling førstegangsbehandling;

    @BeforeEach
    void setUp() {
        var repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        behandlingRepository = repositoryProvider.getBehandlingRepository();
        relevanteKontrollperioderUtleder = mock(RelevanteKontrollperioderUtleder.class);
        henleggBehandlingTjeneste = mock(HenleggBehandlingTjeneste.class);
        task = new HenleggKontrollbehandlingUtenMånederTask(null, null, behandlingRepository, relevanteKontrollperioderUtleder, henleggBehandlingTjeneste);
        førstegangsbehandling = TestScenarioBuilder.builderMedSøknad().lagre(repositoryProvider);
    }

    @Test
    void henlegger_kontrollbehandling_uten_måneder_å_kontrollere() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL);
        when(relevanteKontrollperioderUtleder.utledPerioderForKontrollAvInntekt(kontroll.getId(), HenleggKontrollbehandlingUtenMånederTask.KONTROLLÅRSAKER))
            .thenReturn(LocalDateTimeline.empty());

        task.prosesser(HenleggKontrollbehandlingUtenMånederTask.lagTask(kontroll.getFagsakId(), kontroll.getId()));

        verify(henleggBehandlingTjeneste).henleggBehandlingOgAksjonspunkter(kontroll.getId().toString(),
            BehandlingResultatType.HENLAGT_MASKINELT, HenleggKontrollbehandlingUtenMånederTask.BEGRUNNELSE);
    }

    @Test
    void henlegger_ikke_kontrollbehandling_med_bare_mottatt_rapportering() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL);
        var august = new LocalDateTimeline<>(java.time.LocalDate.of(2025, 8, 1), java.time.LocalDate.of(2025, 8, 31),
            Set.of(BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT));
        when(relevanteKontrollperioderUtleder.utledPerioderForKontrollAvInntekt(kontroll.getId(), HenleggKontrollbehandlingUtenMånederTask.KONTROLLÅRSAKER))
            .thenReturn(august);

        task.prosesser(HenleggKontrollbehandlingUtenMånederTask.lagTask(kontroll.getFagsakId(), kontroll.getId()));

        verify(henleggBehandlingTjeneste, never()).henleggBehandlingOgAksjonspunkter(anyString(), any(), anyString());
    }

    @Test
    void gjør_ingenting_når_kontrollbehandlingen_allerede_er_avsluttet() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL);
        Whitebox.setInternalState(kontroll, "status", BehandlingStatus.AVSLUTTET);
        lagreOgFlush(kontroll);

        task.prosesser(HenleggKontrollbehandlingUtenMånederTask.lagTask(kontroll.getFagsakId(), kontroll.getId()));

        verifyNoInteractions(relevanteKontrollperioderUtleder, henleggBehandlingTjeneste);
    }

    @Test
    void feiler_for_behandling_som_ikke_er_kontrollbehandling() {
        var revurdering = nyBehandling(BehandlingType.REVURDERING);
        var prosessTask = HenleggKontrollbehandlingUtenMånederTask.lagTask(revurdering.getFagsakId(), revurdering.getId());

        assertThatThrownBy(() -> task.prosesser(prosessTask)).isInstanceOf(IllegalStateException.class);

        verify(henleggBehandlingTjeneste, never()).henleggBehandlingOgAksjonspunkter(anyString(), any(), anyString());
    }

    @Test
    void tasken_er_knyttet_til_fagsaken_og_låser_kontrollbehandlingen() {
        var kontroll = nyBehandling(BehandlingType.KONTROLL);

        var prosessTask = HenleggKontrollbehandlingUtenMånederTask.lagTask(kontroll.getFagsakId(), kontroll.getId());

        assertThat(prosessTask.getBehandlingId()).isNull();
        assertThat(task.identifiserBehandling(prosessTask)).isEqualTo(List.of(kontroll.getId().toString()));
    }

    private Behandling nyBehandling(BehandlingType type) {
        var behandling = Behandling.fraTidligereBehandling(førstegangsbehandling, type).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        new InternalManipulerBehandling().forceOppdaterBehandlingSteg(behandling, VURDER_KOMPLETTHET);
        lagreOgFlush(behandling);
        return behandling;
    }

    private void lagreOgFlush(Behandling behandling) {
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        entityManager.flush();
    }
}
