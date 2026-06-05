package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.BEHANDLING_ÅRSAK;
import static no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask.PERIODER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VarselOpphørVedMaksdatoTaskTest {

    private VarselOpphørVedMaksdatoTask task;
    private BehandlingRepository behandlingRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private AktuelleFagsakerForMaksdatoVarselRepository aktuellFagsakRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @BeforeEach
    void setUp() {
        behandlingRepository = mock(BehandlingRepository.class);
        etterlysningRepository = mock(EtterlysningRepository.class);
        etterlysningRepository = mock(EtterlysningRepository.class);
        aktuellFagsakRepository = mock(AktuelleFagsakerForMaksdatoVarselRepository.class);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        ungdomsprogramPeriodeTjeneste = mock(UngdomsprogramPeriodeTjeneste.class);

        task = new VarselOpphørVedMaksdatoTask(
            behandlingRepository,
            prosessTaskTjeneste,
            ungdomsprogramPeriodeTjeneste,
            aktuellFagsakRepository
        );
    }

    @Test
    void skal_opprette_revurdering_nar_maksdato_er_innenfor_3_uker() {
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);
        var maksdato = LocalDate.now().plusWeeks(2);

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of());
        when(etterlysningRepository.hentSisteEtterlysning(eq(100L), eq(EtterlysningType.UTTALELSE_OPPHOR_VED_MAKSDATO), any(), any())).thenReturn(Optional.empty());
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.of(maksdato));

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getTasks()).hasSize(1);
    }

    @Test
    void skal_ikke_opprette_revurdering_nar_maksdato_er_mer_enn_3_uker_frem() {
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);
        var maksdato = LocalDate.now().plusWeeks(4);

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of());
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.of(maksdato));

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_opprette_revurdering_nar_maksdato_nettopp_er_passert_innenfor_grace_periode() {
        // Innenfor 3 dagers grace — tasken kan ha vært i FEILET
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);
        var maksdato = LocalDate.now().minusDays(2);

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of());
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.of(maksdato));

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getTasks()).hasSize(1);
    }

    @Test
    void skal_ikke_opprette_revurdering_nar_maksdato_er_passert_utenfor_grace_periode() {
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);
        var maksdato = LocalDate.now().minusDays(4); // 4 dager siden — utenfor 3 dagers grace

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of());
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.of(maksdato));

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_ikke_dobbelvarsle_nar_varsel_behandling_er_avsluttet_nylig() {
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);
        var maksdato = LocalDate.now().plusWeeks(2);

        var tidligereVarselBehandling = mock(Behandling.class);
        when(tidligereVarselBehandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO)).thenReturn(true);
        when(tidligereVarselBehandling.getOpprettetTidspunkt()).thenReturn(LocalDateTime.now().minusHours(2));

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of(tidligereVarselBehandling));
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.of(maksdato));

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_ikke_opprette_revurdering_nar_grunnlag_ikke_har_maksdato() {
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(1L)).thenReturn(List.of());
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.empty());

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }


    @SuppressWarnings("unchecked")
    private void mockLøpendeFagsaker(List<Fagsak> fagsaker) {
        when(aktuellFagsakRepository.hentFagsakerRelevantForMaksdatoVarsel()).thenReturn(fagsaker);
    }

    private Fagsak lagFagsak(Long id, String aktørId) {
        var fagsak = mock(Fagsak.class);
        when(fagsak.getId()).thenReturn(id);
        when(fagsak.getAktørId()).thenReturn(new AktørId(aktørId));
        when(fagsak.getPeriode()).thenReturn(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 12, 31)));
        when(fagsak.getYtelseType()).thenReturn(FagsakYtelseType.UNGDOMSYTELSE);
        when(fagsak.getStatus()).thenReturn(FagsakStatus.LØPENDE);
        return fagsak;
    }

    private Behandling lagBehandling(Fagsak fagsak, Long behandlingId) {
        var behandling = mock(Behandling.class);
        when(behandling.getId()).thenReturn(behandlingId);
        when(behandling.getFagsak()).thenReturn(fagsak);
        return behandling;
    }

    private ProsessTaskData lagVentendeOpprettRevurderingTask(String årsak, String perioder) {
        var taskData = ProsessTaskData.forProsessTask(OpprettRevurderingEllerOpprettDiffTask.class);
        taskData.setProperty(BEHANDLING_ÅRSAK, årsak);
        taskData.setProperty(PERIODER, perioder);
        return taskData;
    }
}
