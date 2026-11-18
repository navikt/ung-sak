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
import java.util.Map;
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
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class MigrerVedtattYtelseTilSamordningTaskTest {

    @Inject
    private EntityManager entityManager;

    private final BehandlingRepository behandlingRepository = mock();
    private final ProsessTaskTjeneste prosessTaskTjeneste = mock();

    private FagsakRepository fagsakRepository;
    private MigrerVedtattYtelseTilSamordningTask task;

    @BeforeEach
    void setUp() {
        fagsakRepository = new FagsakRepository(entityManager);
        task = new MigrerVedtattYtelseTilSamordningTask(entityManager, behandlingRepository, prosessTaskTjeneste);
    }

    @Test
    void skal_opprette_én_sekvensiell_gruppe_per_fagsak_med_eldste_vedtak_først() {
        var upy = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE);
        var avp = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER);
        medBehandlinger(upy, 102L, 101L);
        medBehandlinger(avp, 201L);

        task.doTask(bolk(upy.getId() - 1, 100, false));

        var grupper = lagredeGrupper();
        assertThat(grupper).hasSize(2);
        assertThat(behandlingIder(grupper.get(0))).containsExactly("101", "102");
        assertThat(behandlingIder(grupper.get(1))).containsExactly("201");
        assertThat(grupper).flatExtracting(MigrerVedtattYtelseTilSamordningTaskTest::taskTyper)
            .containsOnly(LagreVedtattYtelseTask.TASKTYPE);
    }

    @Test
    void skal_kjøre_behandlingene_i_en_fagsak_sekvensielt() {
        var upy = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE);
        medBehandlinger(upy, 102L, 101L);

        task.doTask(bolk(upy.getId() - 1, 100, false));

        var sekvenser = lagredeGrupper().getFirst().getTasks().stream().map(ProsessTaskGruppe.Entry::getSekvens).toList();
        assertThat(sekvenser).doesNotHaveDuplicates();
    }

    @Test
    void skal_ikke_ta_med_fagsaker_for_andre_ytelser() {
        var annen = lagFagsak(FagsakYtelseType.OMSORGSPENGER);
        medBehandlinger(annen, 301L);

        task.doTask(bolk(annen.getId() - 1, 100, false));

        verify(behandlingRepository, never()).finnAlleAvsluttedeIkkeHenlagteYtelsebehandlinger(annen.getId());
    }

    @Test
    void skal_hoppe_over_fagsaker_uten_avsluttede_behandlinger() {
        var utenVedtak = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER);
        var medVedtak = lagFagsak(FagsakYtelseType.AKTIVITETSPENGER);
        medBehandlinger(medVedtak, 401L);

        task.doTask(bolk(utenVedtak.getId() - 1, 100, false));

        assertThat(lagredeGrupper()).singleElement()
            .satisfies(gruppe -> assertThat(behandlingIder(gruppe)).containsExactly("401"));
    }

    @Test
    void skal_ikke_opprette_tasker_ved_dryrun() {
        var upy = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE);
        medBehandlinger(upy, 101L);

        task.doTask(bolk(upy.getId() - 1, 1, true));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskData.class));
    }

    @Test
    void skal_opprette_neste_bolk_når_bolken_er_full() {
        var første = lagFagsak(FagsakYtelseType.UNGDOMSYTELSE);
        lagFagsak(FagsakYtelseType.UNGDOMSYTELSE);

        task.doTask(bolk(første.getId() - 1, 1, false));

        var captor = ArgumentCaptor.forClass(ProsessTaskData.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var nesteBolk = captor.getValue();
        assertThat(nesteBolk.getTaskType()).isEqualTo(MigrerVedtattYtelseTilSamordningTask.TASKTYPE);
        assertThat(nesteBolk.getPropertyValue(MigrerVedtattYtelseTilSamordningTask.FRA_OG_MED_FAGSAK_ID)).isEqualTo(Long.toString(første.getId()));
        assertThat(nesteBolk.getPropertyValue(MigrerVedtattYtelseTilSamordningTask.ANTALL)).isEqualTo("1");
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
                return behandling;
            })
            .toList();
        when(behandlingRepository.finnAlleAvsluttedeIkkeHenlagteYtelsebehandlinger(fagsak.getId())).thenReturn(behandlinger);
    }

    private List<ProsessTaskGruppe> lagredeGrupper() {
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste, atLeast(0)).lagre(captor.capture());
        return captor.getAllValues();
    }

    private static List<String> behandlingIder(ProsessTaskGruppe gruppe) {
        return gruppe.getTasks().stream().map(e -> e.getTask().getBehandlingId()).toList();
    }

    private static List<String> taskTyper(ProsessTaskGruppe gruppe) {
        return gruppe.getTasks().stream().map(e -> e.getTask().getTaskType()).toList();
    }

    private static ProsessTaskData bolk(long fraOgMedFagsakId, int antall, boolean dryRun) {
        var taskData = ProsessTaskData.forProsessTask(MigrerVedtattYtelseTilSamordningTask.class);
        Map.of(
            MigrerVedtattYtelseTilSamordningTask.FRA_OG_MED_FAGSAK_ID, Long.toString(fraOgMedFagsakId),
            MigrerVedtattYtelseTilSamordningTask.ANTALL, Integer.toString(antall),
            MigrerVedtattYtelseTilSamordningTask.DRY_RUN, Boolean.toString(dryRun)
        ).forEach(taskData::setProperty);
        return taskData;
    }
}
