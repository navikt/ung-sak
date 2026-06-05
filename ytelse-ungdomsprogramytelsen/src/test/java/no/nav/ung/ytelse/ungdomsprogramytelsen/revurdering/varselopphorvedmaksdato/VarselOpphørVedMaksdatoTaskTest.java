package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselopphorvedmaksdato;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VarselOpphørVedMaksdatoTaskTest {

    private VarselOpphørVedMaksdatoTask task;
    private BehandlingRepository behandlingRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private AktuelleFagsakerForMaksdatoVarselRepository aktuellFagsakRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @BeforeEach
    void setUp() {
        behandlingRepository = mock(BehandlingRepository.class);
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
    void skal_opprette_revurdering_for_fagsak_fra_repository() {
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);
        var maksdato = LocalDate.now().plusWeeks(2);

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.of(maksdato));

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getTasks()).hasSize(1);
        verify(aktuellFagsakRepository).hentFagsakerRelevantForMaksdatoVarsel();
    }

    @Test
    void skal_ikke_lagre_taskgruppe_nar_repository_returnerer_tom_liste() {
        mockLøpendeFagsaker(List.of());

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_ikke_opprette_task_nar_siste_ytelsesbehandling_ikke_finnes() {
        var fagsak = lagFagsak(1L, "1234567890123");

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.empty());

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_opprette_task_nar_maksdato_mangler_i_grunnlaget() {
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.empty());

        task.doTask(ProsessTaskData.forProsessTask(VarselOpphørVedMaksdatoTask.class));

        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        assertThat(captor.getValue().getTasks()).hasSize(1);
    }

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

}
