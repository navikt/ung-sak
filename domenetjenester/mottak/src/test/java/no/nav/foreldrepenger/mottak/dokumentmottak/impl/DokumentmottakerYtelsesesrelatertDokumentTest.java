package no.nav.foreldrepenger.mottak.dokumentmottak.impl;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.mottak.Behandlingsoppretter;
import no.nav.foreldrepenger.mottak.dokumentmottak.HistorikkinnslagTjeneste;
import no.nav.foreldrepenger.mottak.dokumentmottak.MottatteDokumentTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.dokument.DokumentTypeId;
import no.nav.k9.kodeverk.produksjonsstyring.OrganisasjonsEnhet;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.domene.uttak.UttakTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.db.Repository;

@RunWith(CdiRunner.class)
public class DokumentmottakerYtelsesesrelatertDokumentTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private Repository repository = repoRule.getRepository();

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Inject
    private BehandlingRepository behandlingRepository;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;
    @Mock
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;
    @Mock
    private DokumentmottakerFelles dokumentmottakerFelles;
    @Mock
    private Kompletthetskontroller kompletthetskontroller;
    @Mock
    private Behandlingsoppretter behandlingsoppretter;
    @Mock
    private HistorikkinnslagTjeneste historikkinnslagTjeneste;
    @Mock
    private UttakTjeneste uttakTjeneste;

    private DokumentmottakerYtelsesesrelatertDokument dokumentmottaker;

    @Before
    public void oppsett() {
        MockitoAnnotations.initMocks(this);

        dokumentmottakerFelles = Mockito.spy(new DokumentmottakerFelles(repositoryProvider,
            prosessTaskRepository,
            behandlendeEnhetTjeneste,
            historikkinnslagTjeneste,
            mottatteDokumentTjeneste,
            behandlingsoppretter));

        dokumentmottaker = Mockito.spy(new DokumentmottakerInntektsmelding(dokumentmottakerFelles,
            mottatteDokumentTjeneste,
            behandlingsoppretter,
            kompletthetskontroller,
            uttakTjeneste,
            repositoryProvider));

        var enhet = new OrganisasjonsEnhet("0312", "enhetNavn");
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFraSøker(any(Fagsak.class))).thenReturn(enhet);
        when(behandlendeEnhetTjeneste.finnBehandlendeEnhetFraSøker(any(Behandling.class))).thenReturn(enhet);
    }

    @Test
    public void skal_opprette_vurder_dokument_oppgave_dersom_avslått_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        var behandling = scenario.lagre(repositoryProvider);
        avsluttBehandling(behandling, VedtakResultatType.AVSLAG);
        behandling = behandlingRepository.hentBehandling(behandling.getId());

        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", dokumentTypeId);
        when(behandlingsoppretter.erAvslåttBehandling(behandling)).thenReturn(true);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        // Assert
        verify(dokumentmottaker).håndterAvslåttEllerOpphørtBehandling(mottattDokument, behandling.getFagsak(), behandling,
            BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereInntektsmelding(behandling.getFagsak(), behandling, mottattDokument);
    }

    @Test
    public void skal_opprette_vurder_dokument_oppgave_dersom_opphørt_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        avsluttBehandling(behandling, VedtakResultatType.OPPHØR);
        behandling = behandlingRepository.hentBehandling(behandling.getId());

        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", dokumentTypeId);
        when(behandlingsoppretter.harBehandlingsresultatOpphørt(behandling)).thenReturn(true);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        // Assert
        verify(dokumentmottaker).håndterAvslåttEllerOpphørtBehandling(mottattDokument, behandling.getFagsak(), behandling,
            BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
        verify(dokumentmottakerFelles).opprettTaskForÅVurdereInntektsmelding(behandling.getFagsak(), behandling, mottattDokument);
    }

    @Test
    public void skal_ikke_opprette_førstegangsbehandling_dersom_opphørt_behandling() {
        // Arrange - opprette avsluttet førstegangsbehandling
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, Utfall.IKKE_OPPFYLT);
        var behandling = scenario.lagre(repositoryProvider);
        behandling.avsluttBehandling();
        avsluttBehandling(behandling, VedtakResultatType.OPPHØR);
        behandling = behandlingRepository.hentBehandling(behandling.getId());

        var dokumentTypeId = DokumentTypeId.INNTEKTSMELDING;
        var mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), "123", dokumentTypeId);
        doReturn(true).when(behandlingsoppretter).harBehandlingsresultatOpphørt(behandling);

        // Act
        dokumentmottaker.mottaDokument(mottattDokument, behandling.getFagsak(), BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        // Assert
        verify(dokumentmottaker).håndterAvslåttEllerOpphørtBehandling(mottattDokument, behandling.getFagsak(), behandling,
            BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
        verify(behandlingsoppretter, times(0)).opprettFørstegangsbehandling(any(), any(), any());
    }

    private void avsluttBehandling(Behandling behandling, VedtakResultatType avslag) {
        behandling.avsluttBehandling();
        var behandlingLås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, behandlingLås);

        var vedtak = DokumentmottakTestUtil.oppdaterVedtaksresultat(behandling, avslag);
        repository.lagre(vedtak);
    }
}
