package no.nav.ung.sak.mottak.dokumentmottak;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.dokument.Brevkode;
import no.nav.ung.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.prosesstask.api.ProsessTaskGruppe;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakProsessTaskRepository;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.mottak.Behandlingsoppretter;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.ung.sak.test.util.UnitTestLookupInstanceImpl;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class InnhentDokumentTjenesteTest {

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;

    private AksjonspunktTestSupport aksjonspunktRepository;

    @Mock
    private ProsessTaskTjeneste prosessTaskTjeneste;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private ProsessTriggereRepository prosessTriggereRepository;
    @Mock
    private Dokumentmottaker dokumentmottaker;
    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;
    @Mock
    private FagsakProsessTaskRepository fagsakProsessTaskRepository;

    private InnhentDokumentTjeneste innhentDokumentTjeneste;

    @BeforeEach
    public void oppsett() {
        aksjonspunktRepository = new AksjonspunktTestSupport();

        MockitoAnnotations.initMocks(this);

        innhentDokumentTjeneste = Mockito.spy(new InnhentDokumentTjeneste(
            new UnitTestLookupInstanceImpl<>(dokumentmottaker),
            behandlingsoppretter,
            repositoryProvider,
            behandlingProsesseringTjeneste,
            prosessTaskTjeneste,
            fagsakProsessTaskRepository,
            prosessTriggereRepository));

        OrganisasjonsEnhet enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(any(Fagsak.class))).thenReturn(enhet);
        when(behandlingProsesseringTjeneste.opprettTaskGruppeForGjenopptaOppdaterFortsett(any(Behandling.class), anyBoolean(), anyBoolean())).thenReturn(new ProsessTaskGruppe());

        when(dokumentmottaker.getTriggere(ArgumentMatchers.anyList())).thenReturn(List.of(new Trigger(DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now(), LocalDate.now()), BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT)));
    }


    @Test
    public void skal_lagre_dokument__dersom_inntektrapportering_på_åpen_behandling() {
        // Arrange - opprette åpen behandling
        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medBehandlingStegStart(BehandlingStegType.INNHENT_REGISTEROPP);
        Behandling behandling = scenario.lagre(repositoryProvider);

        // Arrange - bygg dok
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);

        // Act
        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(mottattDokument));

        // Assert - sjekk flyt
        verify(behandlingProsesseringTjeneste).opprettTaskGruppeForGjenopptaOppdaterFortsett(behandling, false, false);
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), behandling);
    }

    @Test
    public void skal_opprette_revurdering_dersom_inntektrapportering_på_avsluttet_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        Behandling behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        BehandlingLås behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        Behandling revurdering = mock(Behandling.class);
        when(revurdering.getId()).thenReturn(10L);
        when(revurdering.getFagsakId()).thenReturn(behandling.getFagsakId());
        when(revurdering.getFagsak()).thenReturn(behandling.getFagsak());
        when(revurdering.getAktørId()).thenReturn(behandling.getAktørId());

        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);
        when(behandlingsoppretter.opprettNyBehandlingFra(behandling, BehandlingÅrsakType.RE_RAPPORTERING_INNTEKT)).thenReturn(revurdering);

        // Act
        innhentDokumentTjeneste.mottaDokument(behandling.getFagsak(), List.of(mottattDokument));

        // Assert
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), revurdering);
    }

    @Test
    public void skal_opprette_førstegangsbehandling() {

        Fagsak fagsak = DokumentmottakTestUtil.byggFagsak(AktørId.dummy(), new Saksnummer("123"), fagsakRepository);
        MottattDokument mottattDokument = DokumentmottakTestUtil.byggMottattDokument(123L, "", now(), "123", Brevkode.UNGDOMSYTELSE_INNTEKTRAPPORTERING);
        Behandling førstegangsbehandling = mock(Behandling.class);
        when(førstegangsbehandling.getFagsak()).thenReturn(fagsak);
        when(førstegangsbehandling.getAktørId()).thenReturn(AktørId.dummy());
        when(førstegangsbehandling.getFagsakId()).thenReturn(fagsak.getId());
        when(behandlingsoppretter.opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty())).thenReturn(førstegangsbehandling);

        // Act
        innhentDokumentTjeneste.mottaDokument(fagsak, List.of(mottattDokument));

        // Assert
        verify(behandlingsoppretter).opprettFørstegangsbehandling(fagsak, BehandlingÅrsakType.UDEFINERT, Optional.empty());
        verify(dokumentmottaker).lagreDokumentinnhold(List.of(mottattDokument), førstegangsbehandling);
    }

}
