package no.nav.k9.sak.mottak.dokumentmottak;

import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.kodeverk.historikk.HistorikkinnslagType;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.kompletthet.KompletthetModell;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.KompletthetsjekkerProvider;
import no.nav.k9.sak.mottak.repo.MottattDokument;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class KompletthetskontrollerTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Mock
    private KompletthetsjekkerProvider kompletthetsjekkerProvider;

    @Mock
    private DokumentmottakerFelles dokumentmottakerFelles;

    @Mock
    private Kompletthetsjekker kompletthetsjekker;

    @Mock
    private MottatteDokumentTjeneste mottatteDokumentTjeneste;

    @Mock
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Mock
    private ProsessTaskRepository prosessTaskRepository;

    private Kompletthetskontroller kompletthetskontroller;
    private Behandling behandling;
    private MottattDokument mottattDokument;

    @BeforeEach
    public void oppsett() {
        MockitoAnnotations.initMocks(this);

        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagMocked();

        // Simuler at provider alltid gir kompletthetssjekker
        when(kompletthetsjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetsjekker);

        KompletthetModell modell = new KompletthetModell(behandlingskontrollTjeneste, kompletthetsjekkerProvider);
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);


        kompletthetskontroller = new Kompletthetskontroller(dokumentmottakerFelles,
            mottatteDokumentTjeneste,
            modell,
            behandlingProsesseringTjeneste,
            prosessTaskRepository,
            skjæringstidspunktTjeneste);

        mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), null, Brevkode.INNTEKTSMELDING);

    }

    @Test
    public void skal_sette_behandling_på_vent_dersom_kompletthet_ikke_er_oppfylt() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, BehandlingStegType.VURDER_KOMPLETTHET);
        Behandling behandling = scenario.lagre(repositoryProvider); // Skulle gjerne mocket, men da funker ikke AP_DEF
        LocalDateTime ventefrist = LocalDateTime.now().plusDays(1);

        when(kompletthetsjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetsjekker);
        when(kompletthetsjekker.vurderForsendelseKomplett(any())).thenReturn(KompletthetResultat.ikkeOppfylt(ventefrist, Venteårsak.AVV_FODSEL));

        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, List.of(mottattDokument));

        verify(behandlingProsesseringTjeneste, times(0)).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), eq(false));
    }

    @Test
    public void skal_beholde_behandling_på_vent_dersom_kompletthet_ikke_er_oppfylt_deretter_slippe_videre() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD);
        Behandling behandling = scenario.lagre(repositoryProvider); // Skulle gjerne mocket, men da funker ikke AP_DEF
        LocalDateTime ventefrist = LocalDateTime.now().plusDays(1);

        when(kompletthetsjekkerProvider.finnKompletthetsjekkerFor(any(), any())).thenReturn(kompletthetsjekker);
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any())).thenReturn(KompletthetResultat.ikkeOppfylt(ventefrist, Venteårsak.AVV_FODSEL));
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.VURDER_UTLAND)).thenReturn(true);

        // Act
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, List.of(mottattDokument));

        // Assert
        verify(behandlingProsesseringTjeneste, times(0)).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), eq(false));

        // Arrange 2
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any())).thenReturn(KompletthetResultat.oppfylt());

        primeProsessTaskRepositoryKompletthetskontroller(behandling);

        // Act 2
        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, List.of(mottattDokument));

        // Assert 2
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

    private void primeProsessTaskRepositoryKompletthetskontroller(Behandling behandling) {
        when(prosessTaskRepository.lagre(any(ProsessTaskData.class))).then(a -> {
            var data = a.getArgument(0, ProsessTaskData.class);
            if (data.getTaskType().equals(KompletthetskontrollerVurderKompletthetTask.TASKTYPE)) {
                new KompletthetskontrollerVurderKompletthetTask(null, null, kompletthetskontroller).doProsesser(data, behandling);
            }
            return null;
        });
    }

    @Test
    public void skal_gjenoppta_behandling_dersom_behandling_er_komplett_og_kompletthet_ikke_passert() {
        // Arrange
        when(kompletthetsjekker.vurderForsendelseKomplett(any())).thenReturn(KompletthetResultat.oppfylt());
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.VURDER_KOMPLETTHET)).thenReturn(false);
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.VURDER_UTLAND)).thenReturn(true);

        primeProsessTaskRepositoryKompletthetskontroller(behandling);

        kompletthetskontroller.persisterDokumentOgVurderKompletthet(behandling, List.of(mottattDokument));

        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

    @Test
    public void skal_opprette_historikkinnslag_for_tidlig_mottatt_søknad() {
        // Arrange
        LocalDateTime frist = LocalDateTime.now().minusSeconds(30);
        when(kompletthetsjekker.vurderSøknadMottatt(any())).thenReturn(KompletthetResultat.oppfylt());
        when(kompletthetsjekker.vurderSøknadMottattForTidlig(any())).thenReturn(KompletthetResultat.ikkeOppfylt(frist, Venteårsak.FOR_TIDLIG_SOKNAD));
        when(kompletthetsjekker.vurderForsendelseKomplett(any())).thenReturn(KompletthetResultat.ikkeOppfylt(frist, Venteårsak.FOR_TIDLIG_SOKNAD));

        // Act
        kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(behandling, List.of(mottattDokument));

        // Assert
        verify(mottatteDokumentTjeneste).persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(behandling, List.of(mottattDokument));
        verify(dokumentmottakerFelles).opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling, HistorikkinnslagType.BEH_VENT, frist,
            Venteårsak.FOR_TIDLIG_SOKNAD);
    }

    @Test
    public void skal_opprette_historikkinnslag_ikke_komplett() {
        // Arrange
        LocalDateTime frist = LocalDateTime.now();
        when(kompletthetsjekker.vurderSøknadMottatt(any())).thenReturn(KompletthetResultat.oppfylt());
        when(kompletthetsjekker.vurderSøknadMottattForTidlig(any())).thenReturn(KompletthetResultat.oppfylt());
        when(kompletthetsjekker.vurderForsendelseKomplett(any())).thenReturn(KompletthetResultat.ikkeOppfylt(frist, Venteårsak.AVV_DOK));
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any())).thenReturn(KompletthetResultat.oppfylt());

        // Act
        kompletthetskontroller.persisterKøetDokumentOgVurderKompletthet(behandling, List.of(mottattDokument));

        // Assert
        verify(mottatteDokumentTjeneste).persisterInntektsmeldingOgKobleMottattDokumentTilBehandling(behandling, List.of(mottattDokument));
        verify(dokumentmottakerFelles).opprettHistorikkinnslagForVenteFristRelaterteInnslag(behandling, HistorikkinnslagType.BEH_VENT, frist,
            Venteårsak.AVV_DOK);
    }
}
