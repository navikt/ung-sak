package no.nav.ung.sak.formidling.bestilling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktTestSupport;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.behandlingslager.formidling.bestilling.*;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.formidling.BrevTestUtils;
import no.nav.ung.sak.formidling.scenarioer.EndringInntektScenarioer;
import no.nav.ung.sak.formidling.scenarioer.FørstegangsbehandlingScenarioer;
import no.nav.ung.sak.formidling.scenarioer.KombinasjonScenarioer;
import no.nav.ung.sak.test.util.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.UngTestScenario;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VurderVedtaksbrevTaskTest {

    @Inject
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    @Inject
    private VedtaksbrevResultatRepository vedtaksbrevResultatRepository;

    @Inject
    private BrevbestillingRepository brevbestillingRepository;

    @Inject
    private ProsessTaskTjeneste prosessTaskTjeneste;

    @Inject
    @ProsessTask(VurderVedtaksbrevTask.TASKTYPE)
    private VurderVedtaksbrevTask task;

    @Inject
    private EntityManager entityManager;

    @Test
    void oppretterBestilling() {
        // Arrange
        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        var scenarioBuilder = FørstegangsbehandlingScenarioer
            .lagAvsluttetStandardBehandling(ungTestRepositories);
        var behandling = scenarioBuilder.getBehandling();

        var prosessTaskData = lagTask(behandling);

        // Act
        task.prosesser(prosessTaskData);

        // Assert
        List<BrevbestillingEntitet> bestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(bestillinger).hasSize(1);
        var bestilling = bestillinger.getFirst();
        assertThat(bestilling.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(bestilling.getDokumentMalType()).isEqualTo(DokumentMalType.INNVILGELSE_DOK);
        assertThat(bestilling.getStatus()).isEqualTo(BrevbestillingStatusType.NY);

        var vedtaksbrevResultater = vedtaksbrevResultatRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).hasSize(1);
        var resultat = vedtaksbrevResultater.getFirst();
        assertThat(resultat.getBrevbestilling().getId()).isEqualTo(bestilling.getId());
        assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.BESTILT);
        assertThat(resultat.getForklaring()).isNotNull();

        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(1);
        var task = tasker.getFirst();
        assertThat(task.getPropertyValue(VedtaksbrevBestillingTask.BREVBESTILLING_ID)).isEqualTo(bestilling.getId().toString());
    }

    @Test
    void oppretterBestillingerVedFlereBrev() {
        UngTestScenario ungTestScenario = KombinasjonScenarioer
            .kombinasjon_endringMedInntektOgFødselAvBarn(LocalDate.of(2025, 11, 1));

        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        var behandling = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestScenario)
            .buildOgLagreMedUng(ungTestRepositories);

        behandling.avsluttBehandling();

        var prosessTaskData = lagTask(behandling);

        // Act
        task.prosesser(prosessTaskData);

        // Assert
        List<BrevbestillingEntitet> bestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(bestillinger).hasSize(2);

        assertThat(bestillinger)
            .anySatisfy(bestilling -> {
                assertThat(bestilling.getBehandlingId()).isEqualTo(behandling.getId());
                assertThat(bestilling.getDokumentMalType()).isEqualTo(DokumentMalType.ENDRING_INNTEKT);
                assertThat(bestilling.getStatus()).isEqualTo(BrevbestillingStatusType.NY);
            })
            .anySatisfy(bestilling -> {
                assertThat(bestilling.getBehandlingId()).isEqualTo(behandling.getId());
                assertThat(bestilling.getDokumentMalType()).isEqualTo(DokumentMalType.ENDRING_BARNETILLEGG);
                assertThat(bestilling.getStatus()).isEqualTo(BrevbestillingStatusType.NY);
            });


        var vedtaksbrevResultater = vedtaksbrevResultatRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).hasSize(2);

        assertThat(vedtaksbrevResultater)
            .anySatisfy(resultat -> {
                assertThat(resultat.getBrevbestilling().getId()).isEqualTo(bestillinger.getFirst().getId());
                assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.BESTILT);
                assertThat(resultat.getForklaring()).isNotNull();
            })
            .anySatisfy(resultat -> {
                assertThat(resultat.getBrevbestilling().getId()).isEqualTo(bestillinger.get(1).getId());
                assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.BESTILT);
                assertThat(resultat.getForklaring()).isNotNull();
            });


        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(2);
        assertThat(tasker).extracting(it -> Long.valueOf(it.getPropertyValue(VedtaksbrevBestillingTask.BREVBESTILLING_ID)))
            .containsExactlyInAnyOrder(bestillinger.get(0).getId(), bestillinger.get(1).getId());
    }

    @Test
    void ingenBestillingHvisIngenBrev() {
        // Arrange
        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        UngTestScenario ungTestScenario = EndringInntektScenarioer.endring0KrInntekt_19år(LocalDate.of(2025, 11, 1));
        var behandling = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestScenario)
            .buildOgLagreMedUng(ungTestRepositories);

        var prosessTaskData = lagTask(behandling);

        // Act
        task.prosesser(prosessTaskData);

        // Assert
        List<BrevbestillingEntitet> bestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(bestillinger).hasSize(0);

        var vedtaksbrevResultater = vedtaksbrevResultatRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).hasSize(1);
        var resultat = vedtaksbrevResultater.getFirst();
        assertThat(resultat.getBrevbestilling()).isNull();
        assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.IKKE_RELEVANT);
        assertThat(resultat.getForklaring()).isNotNull();

        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(0);
    }

    @NotNull
    private static ProsessTaskData lagTask(Behandling behandling) {
        var prosessTaskData = ProsessTaskData.forProsessTask(VurderVedtaksbrevTask.class);
        prosessTaskData.setBehandling(behandling.getFagsakId(), behandling.getId(), behandling.getAktørId().getId());
        return prosessTaskData;
    }

    @Test
    void ingenBestillingHvisHindret() {
        // Arrange
        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        var scenarioBuilder = FørstegangsbehandlingScenarioer
            .lagAvsluttetStandardBehandling(ungTestRepositories);
        var behandling = scenarioBuilder.getBehandling();

        vedtaksbrevValgRepository.lagre(
            new VedtaksbrevValgEntitet(behandling.getId(), false, true, null)
        );

        var prosessTaskData = lagTask(behandling);

        // Act
        task.prosesser(prosessTaskData);

        // Assert
        List<BrevbestillingEntitet> bestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(bestillinger).hasSize(0);

        var vedtaksbrevResultater = vedtaksbrevResultatRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).hasSize(1);
        var resultat = vedtaksbrevResultater.getFirst();
        assertThat(resultat.getBrevbestilling()).isNull();
        assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.HINDRET_SAKSBEHANDLER);
        assertThat(resultat.getForklaring()).isNull();

        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(0);
    }

    @Test
    void manuellBrevHvisRedigert() {
        // Arrange
        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        UngTestScenario ungTestScenario = EndringInntektScenarioer.endring0KrInntekt_19år(LocalDate.of(2025, 11, 1));
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad().medUngTestGrunnlag(ungTestScenario);
        var behandling = lagAvsluttetBehandlingMedAksjonspunkt(scenarioBuilder, ungTestRepositories);

        vedtaksbrevValgRepository.lagre(
            new VedtaksbrevValgEntitet(behandling.getId(), true, false, "tekst")
        );

        var prosessTaskData = lagTask(behandling);

        // Act
        task.prosesser(prosessTaskData);

        // Assert
        List<BrevbestillingEntitet> bestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(bestillinger).hasSize(1);
        var bestilling = bestillinger.getFirst();
        assertThat(bestilling.getDokumentMalType()).isEqualTo(DokumentMalType.MANUELT_VEDTAK_DOK);
        assertThat(bestilling.getStatus()).isEqualTo(BrevbestillingStatusType.NY);

        var vedtaksbrevResultater = vedtaksbrevResultatRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).hasSize(1);
        var resultat = vedtaksbrevResultater.getFirst();
        assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.BESTILT);
        assertThat(resultat.getForklaring()).isNotNull();

        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(1);
        var task = tasker.getFirst();
        assertThat(task.getPropertyValue(VedtaksbrevBestillingTask.BREVBESTILLING_ID)).isEqualTo(bestilling.getId().toString());
    }

    @NotNull
    private static Behandling lagAvsluttetBehandlingMedAksjonspunkt(TestScenarioBuilder scenarioBuilder, UngTestRepositories ungTestRepositories) {
        scenarioBuilder.leggTilAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_INNTEKT, BehandlingStegType.KONTROLLER_REGISTER_INNTEKT);
        var behandling = scenarioBuilder.buildOgLagreMedUng(ungTestRepositories);
        Aksjonspunkt aksjonspunkt = behandling.getAksjonspunktFor(AksjonspunktDefinisjon.KONTROLLER_INNTEKT);
        new AksjonspunktTestSupport().setTilUtført(aksjonspunkt, "utført");

        behandling.avsluttBehandling();
        var behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling;
    }
}
