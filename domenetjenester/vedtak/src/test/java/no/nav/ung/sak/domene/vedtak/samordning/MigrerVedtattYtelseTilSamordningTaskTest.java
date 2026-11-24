package no.nav.ung.sak.domene.vedtak.samordning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.vedtak.IverksettingStatus;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.ung.sak.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.samordning.VedtakYtelseRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class MigrerVedtattYtelseTilSamordningTaskTest {

    @Inject
    private EntityManager entityManager;

    private final BehandlingRepository behandlingRepository = mock();
    private final BehandlingVedtakRepository behandlingVedtakRepository = mock();
    private final VedtakYtelseRepository vedtakYtelseRepository = mock();
    private final ProsessTaskTjeneste prosessTaskTjeneste = mock();

    private FagsakRepository fagsakRepository;
    private MigrerVedtattYtelseTilSamordningTask task;

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(entityManager);
        task = new MigrerVedtattYtelseTilSamordningTask(entityManager, behandlingRepository, behandlingVedtakRepository, vedtakYtelseRepository, prosessTaskTjeneste);
    }

    @Test
    void skal_opprette_én_lagringstask_per_fagsak_for_siste_vedtak() {
        var upy = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE);
        var avp = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER);
        medBehandlinger(upy, 102L, 101L);
        medBehandlinger(avp, 201L);

        task.doTask(migrering(false));

        var tasker = lagredeTasker();
        assertThat(tasker).extracting(ProsessTaskData::getBehandlingId).containsExactlyInAnyOrder("102", "201");
        assertThat(tasker).extracting(ProsessTaskData::getTaskType).containsOnly(LagreVedtattYtelseTask.TASKTYPE);
    }

    @Test
    void skal_lagre_alle_lagringstaskene_parallelt_i_én_gruppe() {
        medBehandlinger(lagFagsak(FagsakYtelseType.UNGDOMSYTELSE), 101L);
        medBehandlinger(lagFagsak(FagsakYtelseType.AKTIVITETSPENGER), 201L);

        task.doTask(migrering(false));

        assertThat(lagredeGrupper()).singleElement().satisfies(gruppe -> {
            assertThat(gruppe.getTasks()).hasSize(2);
            assertThat(gruppe.getTasks()).extracting(ProsessTaskGruppe.Entry::getSekvens).containsOnly(gruppe.getTasks().getFirst().getSekvens());
        });
    }

    @Test
    void skal_hoppe_over_saker_der_siste_vedtak_allerede_er_lagret() {
        var lagret = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE);
        var nyereEnnLagret = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER);
        medBehandlinger(lagret, 101L);
        medBehandlinger(nyereEnnLagret, 202L, 201L);
        when(vedtakYtelseRepository.hentVedtakReferanser(MigrerVedtattYtelseTilSamordningTask.YTELSER)).thenReturn(Set.of(uuid(101L), uuid(201L)));

        task.doTask(migrering(false));

        assertThat(lagredeTasker()).extracting(ProsessTaskData::getBehandlingId).containsExactly("202");
    }

    @Test
    void skal_ikke_lagre_tom_gruppe_når_alle_vedtak_er_lagret() {
        medBehandlinger(lagFagsak(FagsakYtelseType.UNGDOMSYTELSE), 101L);
        when(vedtakYtelseRepository.hentVedtakReferanser(MigrerVedtattYtelseTilSamordningTask.YTELSER)).thenReturn(Set.of(uuid(101L)));

        task.doTask(migrering(false));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_ikke_ta_med_fagsaker_for_andre_ytelser() {
        var annen = lagFagsak(FagsakYtelseType.OMSORGSPENGER);
        medBehandlinger(annen, 301L);

        task.doTask(migrering(false));

        verify(behandlingRepository, never()).finnAlleAvsluttedeIkkeHenlagteYtelsebehandlinger(annen.getId());
        assertThat(lagredeTasker()).isEmpty();
    }

    @Test
    void skal_hoppe_over_fagsaker_uten_avsluttede_behandlinger() {
        lagFagsak(FagsakYtelseType.AKTIVITETSPENGER);
        var medVedtak = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER);
        medBehandlinger(medVedtak, 401L);

        task.doTask(migrering(false));

        assertThat(lagredeTasker()).extracting(ProsessTaskData::getBehandlingId).containsExactly("401");
    }

    @Test
    void skal_velge_siste_iverksatte_vedtak() {
        var delvisIverksatt = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE);
        var ikkeIverksatt = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER);
        medBehandlinger(delvisIverksatt, 503L, 502L, 501L);
        medIverksettingStatus(503L, IverksettingStatus.IKKE_IVERKSATT);
        medBehandlinger(ikkeIverksatt, 601L);
        medIverksettingStatus(601L, IverksettingStatus.UNDER_IVERKSETTING);

        task.doTask(migrering(false));

        assertThat(lagredeTasker()).extracting(ProsessTaskData::getBehandlingId).containsExactly("502");
    }

    @Test
    void skal_ikke_opprette_lagringstasker_ved_dryrun() {
        var upy = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE);
        medBehandlinger(upy, 101L);

        task.doTask(migrering(true));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    private Fagsak lagFagsak(FagsakYtelseType ytelseType) {
        var saksnummer = new Saksnummer("MIG" + ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
        var fagsak = Fagsak.opprettNy(ytelseType, AktørId.dummy(), saksnummer);
        fagsakRepository.opprettNy(fagsak);
        entityManager.flush();
        return fagsak;
    }

    private void medBehandlinger(Fagsak fagsak, Long... behandlingIderNyesteFørst) {
        var behandlinger = Arrays.stream(behandlingIderNyesteFørst)
            .map(id -> {
                Behandling behandling = mock();
                when(behandling.getId()).thenReturn(id);
                when(behandling.getUuid()).thenReturn(uuid(id));
                medIverksettingStatus(id, IverksettingStatus.IVERKSATT);
                return behandling;
            })
            .toList();
        when(behandlingRepository.finnAlleAvsluttedeIkkeHenlagteYtelsebehandlinger(fagsak.getId())).thenReturn(behandlinger);
    }

    private void medIverksettingStatus(Long behandlingId, IverksettingStatus status) {
        BehandlingVedtak vedtak = mock();
        when(vedtak.getIverksettingStatus()).thenReturn(status);
        when(behandlingVedtakRepository.hentBehandlingVedtakForBehandlingId(behandlingId)).thenReturn(Optional.of(vedtak));
    }

    private List<ProsessTaskGruppe> lagredeGrupper() {
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste, atLeast(0)).lagre(captor.capture());
        return captor.getAllValues();
    }

    private List<ProsessTaskData> lagredeTasker() {
        return lagredeGrupper().stream().flatMap(g -> g.getTasks().stream()).map(ProsessTaskGruppe.Entry::getTask).toList();
    }

    private static UUID uuid(long behandlingId) {
        return new UUID(0L, behandlingId);
    }

    private static ProsessTaskData migrering(boolean dryRun) {
        var taskData = ProsessTaskData.forProsessTask(MigrerVedtattYtelseTilSamordningTask.class);
        taskData.setProperty(MigrerVedtattYtelseTilSamordningTask.DRY_RUN, Boolean.toString(dryRun));
        return taskData;
    }
}
