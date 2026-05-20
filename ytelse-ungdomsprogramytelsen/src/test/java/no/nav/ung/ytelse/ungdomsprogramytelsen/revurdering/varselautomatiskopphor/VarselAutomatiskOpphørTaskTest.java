package no.nav.ung.ytelse.ungdomsprogramytelsen.revurdering.varselautomatiskopphor;

import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.behandling.FagsakStatus;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.revurdering.OpprettRevurderingEllerOpprettDiffTask;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VarselAutomatiskOpphørTaskTest {

    private VarselAutomatiskOpphørTask task;
    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private EtterlysningRepository etterlysningRepository;
    private ProsessTaskTjeneste prosessTaskTjeneste;
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;
    private UngdomsprogramPeriodeTjeneste ungdomsprogramPeriodeTjeneste;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        behandlingRepository = mock(BehandlingRepository.class);
        etterlysningRepository = mock(EtterlysningRepository.class);
        prosessTaskTjeneste = mock(ProsessTaskTjeneste.class);
        fagsakProsessTaskRepository = mock(FagsakProsessTaskRepository.class);
        ungdomsprogramPeriodeTjeneste = mock(UngdomsprogramPeriodeTjeneste.class);

        task = new VarselAutomatiskOpphørTask(
            entityManager,
            behandlingRepository,
            etterlysningRepository,
            prosessTaskTjeneste,
            fagsakProsessTaskRepository,
            ungdomsprogramPeriodeTjeneste
        );
    }

    @Test
    void skal_opprette_revurdering_når_maksdato_er_innenfor_4_uker() {
        // Arrange
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);
        var maksdato = LocalDate.now().plusWeeks(2); // 2 uker fra nå — innenfor vinduet

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(1L)).thenReturn(List.of());
        when(etterlysningRepository.hentSisteEtterlysning(eq(100L), eq(EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR), any(), any())).thenReturn(Optional.empty());
        when(fagsakProsessTaskRepository.finnAlleForAngittSøk(eq(1L), any(), any(), any(), anyBoolean())).thenReturn(List.of());
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.of(maksdato));

        // Act
        task.doTask(ProsessTaskData.forProsessTask(VarselAutomatiskOpphørTask.class));

        // Assert
        var captor = ArgumentCaptor.forClass(ProsessTaskGruppe.class);
        verify(prosessTaskTjeneste).lagre(captor.capture());
        var gruppe = captor.getValue();
        assertThat(gruppe.getTasks()).hasSize(1);
    }

    @Test
    void skal_ikke_opprette_revurdering_når_maksdato_er_mer_enn_4_uker_frem() {
        // Arrange
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);
        var maksdato = LocalDate.now().plusWeeks(6); // 6 uker — utenfor vinduet

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(1L)).thenReturn(List.of());
        when(etterlysningRepository.hentSisteEtterlysning(eq(100L), eq(EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR), any(), any())).thenReturn(Optional.empty());
        when(fagsakProsessTaskRepository.finnAlleForAngittSøk(eq(1L), any(), any(), any(), anyBoolean())).thenReturn(List.of());
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.of(maksdato));

        // Act
        task.doTask(ProsessTaskData.forProsessTask(VarselAutomatiskOpphørTask.class));

        // Assert
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_ikke_opprette_revurdering_når_varsel_allerede_finnes() {
        // Arrange
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);
        var varselBehandling = mock(Behandling.class);
        when(varselBehandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_VARSEL_AUTOMATISK_OPPHOR)).thenReturn(true);

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(1L)).thenReturn(List.of(varselBehandling));

        // Act
        task.doTask(ProsessTaskData.forProsessTask(VarselAutomatiskOpphørTask.class));

        // Assert
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @Test
    void skal_ikke_opprette_revurdering_når_grunnlag_ikke_har_maksdato() {
        // Arrange
        var fagsak = lagFagsak(1L, "1234567890123");
        var behandling = lagBehandling(fagsak, 100L);

        mockLøpendeFagsaker(List.of(fagsak));
        when(behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(1L)).thenReturn(Optional.of(behandling));
        when(behandlingRepository.hentBehandlingerSomIkkeErAvsluttetForFagsakId(1L)).thenReturn(List.of());
        when(etterlysningRepository.hentSisteEtterlysning(eq(100L), eq(EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR), any(), any())).thenReturn(Optional.empty());
        when(fagsakProsessTaskRepository.finnAlleForAngittSøk(eq(1L), any(), any(), any(), anyBoolean())).thenReturn(List.of());
        // Grunnlag returnerer tom maksdato
        when(ungdomsprogramPeriodeTjeneste.finnPeriodeMaksDato(100L)).thenReturn(Optional.empty());

        // Act
        task.doTask(ProsessTaskData.forProsessTask(VarselAutomatiskOpphørTask.class));

        // Assert
        verify(prosessTaskTjeneste, never()).lagre(any(ProsessTaskGruppe.class));
    }

    @SuppressWarnings("unchecked")
    private void mockLøpendeFagsaker(List<Fagsak> fagsaker) {
        TypedQuery<Fagsak> query = mock(TypedQuery.class);
        when(entityManager.createQuery(anyString(), eq(Fagsak.class))).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(fagsaker);
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
