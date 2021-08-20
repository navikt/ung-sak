package no.nav.k9.sak.mottak.dokumentmottak;

import static java.time.LocalDate.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.Venteårsak;
import no.nav.k9.kodeverk.dokument.Brevkode;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskRepository;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.prosessering.BehandlingProsesseringTjeneste;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.motattdokument.MottattDokument;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.kompletthet.KompletthetModell;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.kompletthet.ManglendeVedlegg;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class KompletthetskontrollerTest {

    private static Kompletthetsjekker kompletthetsjekker;
    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    @Mock
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    @Mock
    private DokumentmottakerFelles dokumentmottakerFelles;
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

        kompletthetsjekker = Mockito.mock(Kompletthetsjekker.class);

        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OBSOLETE);
        behandling = scenario.lagMocked();

        KompletthetModell modell = new KompletthetModell(behandlingskontrollTjeneste);
        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste = Mockito.mock(SkjæringstidspunktTjeneste.class);

        kompletthetskontroller = new Kompletthetskontroller(
                modell,
            behandlingProsesseringTjeneste
        );

        mottattDokument = DokumentmottakTestUtil.byggMottattDokument(behandling.getFagsakId(), "", now(), null, Brevkode.INNTEKTSMELDING);

    }

    @Test
    public void skal_sette_behandling_på_vent_dersom_kompletthet_ikke_er_oppfylt() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, BehandlingStegType.VURDER_KOMPLETTHET);
        Behandling behandling = scenario.lagre(repositoryProvider); // Skulle gjerne mocket, men da funker ikke AP_DEF
        LocalDateTime ventefrist = LocalDateTime.now().plusDays(1);

        when(kompletthetsjekker.vurderForsendelseKomplett(any())).thenReturn(KompletthetResultat.ikkeOppfylt(ventefrist, Venteårsak.AVV_DOK));

        var prosessTaskData = kompletthetskontroller.asynkVurderKompletthet(behandling);
        prosessTaskRepository.lagre(prosessTaskData);

        verify(behandlingProsesseringTjeneste, times(0)).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), eq(false));
    }

    @Test
    public void skal_beholde_behandling_på_vent_dersom_kompletthet_ikke_er_oppfylt_deretter_slippe_videre() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad(FagsakYtelseType.OBSOLETE);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_ETTERLYST_INNTEKTSMELDING, BehandlingStegType.KONTROLLER_FAKTA_ARBEIDSFORHOLD);
        Behandling behandling = scenario.lagre(repositoryProvider); // Skulle gjerne mocket, men da funker ikke AP_DEF
        LocalDateTime ventefrist = LocalDateTime.now().plusDays(1);

        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any())).thenReturn(KompletthetResultat.ikkeOppfylt(ventefrist, Venteårsak.AVV_DOK));
        when(behandlingskontrollTjeneste.erStegPassert(behandling.getId(), BehandlingStegType.VURDER_UTLAND)).thenReturn(true);

        // Act
        var prosessTaskData1 = kompletthetskontroller.asynkVurderKompletthet(behandling);
        prosessTaskRepository.lagre(prosessTaskData1);

        // Assert
        verify(behandlingProsesseringTjeneste, times(0)).opprettTasksForGjenopptaOppdaterFortsett(eq(behandling), eq(false));

        // Arrange 2
        when(kompletthetsjekker.vurderEtterlysningInntektsmelding(any())).thenReturn(KompletthetResultat.oppfylt());

        primeProsessTaskRepositoryKompletthetskontroller(behandling);

        // Act 2
        var prosessTaskData = kompletthetskontroller.asynkVurderKompletthet(behandling);
        prosessTaskRepository.lagre(prosessTaskData);

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

        var prosessTaskData = kompletthetskontroller.asynkVurderKompletthet(behandling);
        assertThat(prosessTaskData.getTaskType()).isEqualTo(KompletthetskontrollerVurderKompletthetTask.TASKTYPE);
        prosessTaskRepository.lagre(prosessTaskData);
        verify(behandlingProsesseringTjeneste).opprettTasksForFortsettBehandling(behandling);
    }

    @ApplicationScoped
    @FagsakYtelseTypeRef("OBSOLETE")
    @BehandlingTypeRef
    static class DummyKompletthetSjekker implements Kompletthetsjekker {

        @Override
        public KompletthetResultat vurderSøknadMottattForTidlig(BehandlingReferanse ref) {
            return kompletthetsjekker.vurderSøknadMottattForTidlig(ref);
        }

        @Override
        public KompletthetResultat vurderForsendelseKomplett(BehandlingReferanse ref) {
            return kompletthetsjekker.vurderForsendelseKomplett(ref);
        }

        @Override
        public List<ManglendeVedlegg> utledAlleManglendeVedleggForForsendelse(BehandlingReferanse ref) {
            return kompletthetsjekker.utledAlleManglendeVedleggForForsendelse(ref);
        }

        @Override
        public List<ManglendeVedlegg> utledAlleManglendeVedleggSomIkkeKommer(BehandlingReferanse ref) {
            return kompletthetsjekker.utledAlleManglendeVedleggSomIkkeKommer(ref);
        }

        @Override
        public boolean erForsendelsesgrunnlagKomplett(BehandlingReferanse ref) {
            return kompletthetsjekker.erForsendelsesgrunnlagKomplett(ref);
        }

    }
}
