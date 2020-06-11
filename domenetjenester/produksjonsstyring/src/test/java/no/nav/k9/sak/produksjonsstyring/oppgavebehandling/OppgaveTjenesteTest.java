package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;

public class OppgaveTjenesteTest {

    private static final Oppgave OPPGAVE = new Oppgave(99L, null, null, null, null,
        "OMS", null, null, null, 1, "4806",
        LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET);

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();

    private OppgaveTjeneste tjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private OppgaveRestKlient oppgaveRestKlient;

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    private Behandling behandling;

    @Before
    public void oppsett() {

        prosessTaskRepository = mock(ProsessTaskRepository.class);
        oppgaveRestKlient = Mockito.mock(OppgaveRestKlient.class);
        oppgaveBehandlingKoblingRepository = spy(new OppgaveBehandlingKoblingRepository(entityManager));
        tjeneste = new OppgaveTjeneste(repositoryProvider, oppgaveBehandlingKoblingRepository, oppgaveRestKlient, prosessTaskRepository);
        lagBehandling();
    }

    private void lagBehandling() {
        TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(repositoryProvider);
        behandling.setBehandlendeEnhet(new OrganisasjonsEnhet("4802", null));
    }

    @Test
    public void skal_opprette_oppgave_basert_på_fagsakId() {
        // Arrange
        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.VURDER_DOKUMENT, "2010", "bla bla", false);

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String) Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String) Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(OppgaveÅrsak.VURDER_DOKUMENT.getKode());
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }


    @Test
    public void skal_hente_oppgave_liste() throws Exception {
        // Arrange
        when(oppgaveRestKlient.finnÅpneOppgaver(any(), eq("OMS"), eq(List.of(OppgaveÅrsak.VURDER_DOKUMENT.getKode())))).thenReturn(List.of(OPPGAVE));
        when(oppgaveRestKlient.finnÅpneOppgaver(any(), eq("OMS"), eq(List.of(OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE.getKode())))).thenReturn(Collections.emptyList());

        // Act
        var harVurderDok = tjeneste.harÅpneOppgaverAvType(behandling.getAktørId(), OppgaveÅrsak.VURDER_DOKUMENT, FagsakYtelseType.PSB);
        var harVurderKY = tjeneste.harÅpneOppgaverAvType(behandling.getAktørId(), OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE, FagsakYtelseType.PSB);

        // Assert
        assertThat(harVurderDok).isTrue();
        assertThat(harVurderKY).isFalse();
    }

    @Test
    public void skal_opprette_oppgave_vurder_konsekvens_basert_på_fagsakId() {
        // Arrange

        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE, "2010", "bla bla", false);

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String) Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String) Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE.getKode());
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void skal_lage_request_som_inneholder_verdier_i_forbindelse_med_manglende_regler() {
        // Arrange

        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        String oppgaveId = tjeneste.opprettOppgaveSakSkalTilInfotrygd(behandling.getId());

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String) Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String) Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(OppgaveÅrsak.BEHANDLE_SAK_IT.getKode());
        assertThat((String) Whitebox.getInternalState(request, "beskrivelse")).isEqualTo("Sak må flyttes til Infotrygd");
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void skal_opprette_oppgave_med_prioritet_og_beskrivelse() {
        // Arrange
        LocalDate forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.GODKJENN_VEDTAK_VL,
            "4321", "noe tekst", true);

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String) Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String) Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(OppgaveÅrsak.GODKJENN_VEDTAK_VL.getKode());
        assertThat((LocalDate) Whitebox.getInternalState(request, "fristFerdigstillelse")).isEqualTo(forventetFrist);
        assertThat((Prioritet) Whitebox.getInternalState(request, "prioritet")).isEqualTo(Prioritet.HOY);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }

    @Test
    public void opprettOppgaveStopUtbetalingAvARENAYtelse() {
        // Arrange

        LocalDate forventetFrist = VirkedagUtil.fomVirkedag(LocalDate.now().plusDays(1));
        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        LocalDate førsteAugust = LocalDate.of(2019, 8, 1);
        String oppgaveId = tjeneste.opprettOppgaveStopUtbetalingAvARENAYtelse(behandling.getId(), førsteAugust);

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String) Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String) Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(OppgaveÅrsak.SETTVENT.getKode());
        assertThat((String) Whitebox.getInternalState(request, "tema")).isEqualTo("STO");
        assertThat((LocalDate) Whitebox.getInternalState(request, "fristFerdigstillelse")).isEqualTo(forventetFrist);
        assertThat((String) Whitebox.getInternalState(request, "beskrivelse")).isEqualTo("Samordning arenaytelse. Vedtak foreldrepenger fra " + førsteAugust);
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }
}
