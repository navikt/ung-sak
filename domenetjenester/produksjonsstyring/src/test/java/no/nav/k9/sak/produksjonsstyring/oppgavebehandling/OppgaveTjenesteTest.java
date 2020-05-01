package no.nav.k9.sak.produksjonsstyring.oppgavebehandling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.produksjonsstyring.OppgaveÅrsak;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.person.tps.TpsTjeneste;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OppgaveRestKlient;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Oppgavestatus;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.OpprettOppgave;
import no.nav.vedtak.felles.integrasjon.oppgave.v1.Prioritet;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskGruppe;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.db.Repository;

public class OppgaveTjenesteTest {

    private static final String FNR = "00000000000";

    private static final Oppgave OPPGAVE = new Oppgave(99L, null, null, null, null,
        "OMS", null, null, null, 1, "4806",
        LocalDate.now().plusDays(1), LocalDate.now(), Prioritet.NORM, Oppgavestatus.AAPNET);

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private final EntityManager entityManager = repoRule.getEntityManager();
    private Repository repository = repoRule.getRepository();

    private OppgaveTjeneste tjeneste;
    private TpsTjeneste tpsTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private OppgaveRestKlient oppgaveRestKlient;

    private OppgaveBehandlingKoblingRepository oppgaveBehandlingKoblingRepository;

    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(entityManager);

    private Behandling behandling;

    @Before
    public void oppsett() {

        tpsTjeneste = mock(TpsTjeneste.class);
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
    public void skal_opprette_oppgave_når_det_ikke_finnes_fra_før() {
        // Arrange
        Long behandlingId = behandling.getId();

        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        // Act
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK_VL);

        // Assert
        List<OppgaveBehandlingKobling> oppgaver = repository.hentAlle(OppgaveBehandlingKobling.class);
        assertThat(oppgaver).hasSize(1);
        List<OppgaveBehandlingKobling> oppgaveBehandlingKoblinger = oppgaveBehandlingKoblingRepository.hentOppgaverRelatertTilBehandling(behandlingId);
        OppgaveBehandlingKobling oppgaveBehandlingKobling = OppgaveBehandlingKobling.getAktivOppgaveMedÅrsak(OppgaveÅrsak.BEHANDLE_SAK_VL, oppgaveBehandlingKoblinger).orElseThrow(
            () -> new IllegalStateException("Mangler AktivOppgaveMedÅrsak"));
        assertThat(oppgaveBehandlingKobling.getOppgaveÅrsak()).isEqualTo(OppgaveÅrsak.BEHANDLE_SAK_VL);
        assertThat(oppgaveBehandlingKobling.getOppgaveId()).isEqualTo(OPPGAVE.getId().toString());

    }

    @Test
    public void skal_ikke_opprette_en_ny_oppgave_av_samme_type_når_det_finnes_fra_før_og_den_ikke_er_ferdigstilt() {
        // Arrange
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK_VL);
        List<OppgaveBehandlingKobling> oppgaver = repository.hentAlle(OppgaveBehandlingKobling.class);
        assertThat(oppgaver).hasSize(1);
        oppgaver.get(0).setFerdigstilt(false);
        repository.lagre(oppgaver.get(0));

        // Act
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK_VL);

        // Assert
        oppgaver = repository.hentAlle(OppgaveBehandlingKobling.class);
        assertThat(oppgaver).hasSize(1);
    }

    @Test
    public void skal_opprette_en_ny_oppgave_når_det_finnes_fra_før_og_den_er_ferdigstilt() {
        // Arrange
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK_VL);
        List<OppgaveBehandlingKobling> oppgaver = repository.hentAlle(OppgaveBehandlingKobling.class);
        assertThat(oppgaver).hasSize(1);
        oppgaver.get(0).setFerdigstilt(true);
        repository.lagre(oppgaver.get(0));

        // Act
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.GODKJENN_VEDTAK_VL);

        // Assert
        oppgaver = repository.hentAlle(OppgaveBehandlingKobling.class);
        assertThat(oppgaver).hasSize(2);
    }

    @Test
    public void skal_kunne_opprette_en_ny_oppgave_med_en_annen_årsak_selv_om_det_finnes_en_aktiv_oppgave() throws Exception {
        // Arrange
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        // Act
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK_VL);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK_VL);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.GODKJENN_VEDTAK_VL);
        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.GODKJENN_VEDTAK_VL);

        // Assert
        List<OppgaveBehandlingKobling> aktiveOppgaver = repository.hentAlle(OppgaveBehandlingKobling.class).stream()
            .filter(oppgave -> !oppgave.isFerdigstilt())
            .collect(Collectors.toList());
        assertThat(aktiveOppgaver).hasSize(2);
    }

    @Test
    public void skal_avslutte_oppgave() {
        // Arrange
        Long behandlingId = behandling.getId();
        when(oppgaveRestKlient.opprettetOppgave(any())).thenReturn(OPPGAVE);

        tjeneste.opprettBasertPåBehandlingId(behandlingId, OppgaveÅrsak.BEHANDLE_SAK_VL);

        // Act
        tjeneste.avslutt(behandlingId, OppgaveÅrsak.BEHANDLE_SAK_VL);

        // Assert
        List<OppgaveBehandlingKobling> oppgaver = repository.hentAlle(OppgaveBehandlingKobling.class);
        OppgaveBehandlingKobling behandlingKobling = oppgaver.get(0);
        assertThat(behandlingKobling.isFerdigstilt()).isTrue();
    }

    @Test
    public void skal_opprette_oppgave_basert_på_fagsakId() {
        // Arrange
        ArgumentCaptor<OpprettOppgave.Builder> captor = ArgumentCaptor.forClass(OpprettOppgave.Builder.class);
        when(oppgaveRestKlient.opprettetOppgave(captor.capture())).thenReturn(OPPGAVE);

        // Act
        String oppgaveId = tjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(behandling.getFagsakId(), OppgaveÅrsak.VURDER_DOKUMENT_VL, "2010", "bla bla", false);

        // Assert
        OpprettOppgave request = captor.getValue().build();
        assertThat((String) Whitebox.getInternalState(request, "saksreferanse")).isEqualTo(behandling.getFagsak().getSaksnummer().getVerdi());
        assertThat((String) Whitebox.getInternalState(request, "oppgavetype")).isEqualTo(OppgaveÅrsak.VURDER_DOKUMENT_VL.getKode());
        assertThat(oppgaveId).isEqualTo(OPPGAVE.getId().toString());
    }


    @Test
    public void skal_hente_oppgave_liste() throws Exception {
        // Arrange
        when(oppgaveRestKlient.finnÅpneOppgaver(any(), eq("OMS"), eq(List.of(OppgaveÅrsak.VURDER_DOKUMENT_VL.getKode())))).thenReturn(List.of(OPPGAVE));
        when(oppgaveRestKlient.finnÅpneOppgaver(any(), eq("OMS"), eq(List.of(OppgaveÅrsak.VURDER_KONSEKVENS_YTELSE.getKode())))).thenReturn(Collections.emptyList());

        // Act
        var harVurderDok = tjeneste.harÅpneOppgaverAvType(behandling.getAktørId(), OppgaveÅrsak.VURDER_DOKUMENT_VL, FagsakYtelseType.PSB);
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
