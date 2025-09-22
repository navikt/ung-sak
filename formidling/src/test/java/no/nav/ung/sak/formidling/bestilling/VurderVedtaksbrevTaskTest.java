package no.nav.ung.sak.formidling.bestilling;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTask;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.k9.prosesstask.api.ProsessTaskStatus;
import no.nav.k9.prosesstask.api.ProsessTaskTjeneste;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VurderVedtaksbrevTaskTest {

    @Inject
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;

    @Inject
    private BehandlingVedtaksbrevRepository behandlingVedtaksbrevRepository;

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

        var vedtaksbrevResultater = behandlingVedtaksbrevRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).hasSize(1);
        var resultat = vedtaksbrevResultater.getFirst();
        assertThat(resultat.getBrevbestilling().getId()).isEqualTo(bestilling.getId());
        assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.BESTILT);
        assertThat(resultat.getBeskrivelse()).isNotNull();

        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(1);
        var task = tasker.getFirst();
        assertBestillingTask(task, bestilling);
    }

    private static void assertBestillingTask(ProsessTaskData task, BrevbestillingEntitet bestilling) {
        assertThat(task.getPropertyValue(VedtaksbrevBestillingTask.BREVBESTILLING_ID)).isEqualTo(bestilling.getId().toString());
        assertThat(task.getGruppe()).contains(BrevbestillingTaskGenerator.FORMIDLING_GRUPPE_PREFIX);
        assertThat(task.getSekvens()).endsWith("-0");
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

        var bestilling1 = bestillinger.stream()
            .filter(b -> b.getDokumentMalType().equals(DokumentMalType.ENDRING_INNTEKT))
            .findFirst()
            .orElseThrow();
        assertThat(bestilling1.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(bestilling1.getStatus()).isEqualTo(BrevbestillingStatusType.NY);

        var bestilling2 = bestillinger.stream()
            .filter(b -> b.getDokumentMalType().equals(DokumentMalType.ENDRING_BARNETILLEGG))
            .findFirst()
            .orElseThrow();
        assertThat(bestilling2.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(bestilling2.getStatus()).isEqualTo(BrevbestillingStatusType.NY);


        var vedtaksbrevResultater = behandlingVedtaksbrevRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).extracting(it -> it.getBrevbestilling().getId())
            .containsExactlyInAnyOrder(bestillinger.get(0).getId(), bestillinger.get(1).getId());
        assertThat(vedtaksbrevResultater).extracting(BehandlingVedtaksbrev::getResultatType)
            .containsExactly(VedtaksbrevResultatType.BESTILT, VedtaksbrevResultatType.BESTILT);
        assertThat(vedtaksbrevResultater).extracting(BehandlingVedtaksbrev::getBeskrivelse)
            .isNotNull();


        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(2);
        assertThat(tasker).extracting(it -> Long.valueOf(it.getPropertyValue(VedtaksbrevBestillingTask.BREVBESTILLING_ID)))
            .containsExactlyInAnyOrder(bestillinger.get(0).getId(), bestillinger.get(1).getId());

        assertThat(tasker).extracting(ProsessTaskData::getSekvens)
            .anyMatch(it -> it.endsWith("-0"));
        assertThat(tasker).extracting(ProsessTaskData::getSekvens)
            .anyMatch(it -> it.endsWith("-1"));

        assertThat(tasker).allSatisfy(pt -> {
            assertThat(pt.getGruppe()).contains(BrevbestillingTaskGenerator.FORMIDLING_GRUPPE_PREFIX);
        });
    }

    @Test
    void oppretterBestillingerVedKombinasjonRedigertOgAutomatisk() {
        UngTestScenario ungTestScenario = KombinasjonScenarioer
            .kombinasjon_endringMedInntektOgFødselAvBarn(LocalDate.of(2025, 11, 1));

        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(ungTestScenario, ungTestRepositories);
        behandling.avsluttBehandling();

        String redigertBrevHtml = "<h1>redigert</h1>";
        vedtaksbrevValgRepository.lagre(
            new VedtaksbrevValgEntitet(behandling.getId(), DokumentMalType.ENDRING_BARNETILLEGG, true, false, redigertBrevHtml)
        );


        var prosessTaskData = lagTask(behandling);

        // Act
        task.prosesser(prosessTaskData);

        // Assert
        List<BrevbestillingEntitet> bestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(bestillinger).hasSize(2);

        var bestilling1 = bestillinger.stream()
            .filter(b -> b.getDokumentMalType().equals(DokumentMalType.ENDRING_INNTEKT))
            .findFirst()
            .orElseThrow();
        assertThat(bestilling1.getStatus()).isEqualTo(BrevbestillingStatusType.NY);

        var bestilling2 = bestillinger.stream()
            .filter(b -> b.getDokumentMalType().equals(DokumentMalType.MANUELT_VEDTAK_DOK))
            .findFirst()
            .orElseThrow();
        assertThat(bestilling2.getStatus()).isEqualTo(BrevbestillingStatusType.NY);

    }

    @Test
    void oppretterBestillingVedKombinasjonHindretOgRedigert() {
        UngTestScenario ungTestScenario = KombinasjonScenarioer
            .kombinasjon_endringMedInntektOgFødselAvBarn(LocalDate.of(2025, 11, 1));

        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        var behandling = EndringInntektScenarioer.lagBehandlingMedAksjonspunktKontrollerInntekt(ungTestScenario, ungTestRepositories);

        behandling.avsluttBehandling();

        String redigertBrevHtml = "<h1>redigert</h1>";
        vedtaksbrevValgRepository.lagre(
            new VedtaksbrevValgEntitet(behandling.getId(), DokumentMalType.ENDRING_BARNETILLEGG, false, true, null)
        );

        vedtaksbrevValgRepository.lagre(
            new VedtaksbrevValgEntitet(behandling.getId(), DokumentMalType.ENDRING_INNTEKT, true, false, redigertBrevHtml)
        );


        var prosessTaskData = lagTask(behandling);

        // Act
        task.prosesser(prosessTaskData);

        // Assert
        List<BrevbestillingEntitet> bestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(bestillinger).hasSize(1);

        var bestilling2 = bestillinger.stream()
            .filter(b -> b.getDokumentMalType().equals(DokumentMalType.MANUELT_VEDTAK_DOK))
            .findFirst()
            .orElseThrow();
        assertThat(bestilling2.getStatus()).isEqualTo(BrevbestillingStatusType.NY);

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

        var vedtaksbrevResultater = behandlingVedtaksbrevRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).hasSize(1);
        var resultat = vedtaksbrevResultater.getFirst();
        assertThat(resultat.getBrevbestilling()).isNull();
        assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.IKKE_RELEVANT);
        assertThat(resultat.getBeskrivelse()).isNotNull();

        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(0);
    }

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
            new VedtaksbrevValgEntitet(behandling.getId(), DokumentMalType.INNVILGELSE_DOK, false, true, null)
        );

        var prosessTaskData = lagTask(behandling);

        // Act
        task.prosesser(prosessTaskData);

        // Assert
        List<BrevbestillingEntitet> bestillinger = brevbestillingRepository.hentForBehandling(behandling.getId());
        assertThat(bestillinger).hasSize(0);

        var vedtaksbrevResultater = behandlingVedtaksbrevRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).hasSize(1);
        var resultat = vedtaksbrevResultater.getFirst();
        assertThat(resultat.getBrevbestilling()).isNull();
        assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.HINDRET_SAKSBEHANDLER);
        assertThat(resultat.getBeskrivelse()).isNull();

        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(0);
    }

    @Test
    void manuellBrevHvisRedigert() {
        // Arrange
        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        UngTestScenario ungTestScenario = EndringInntektScenarioer.endringMedInntektPå10k_19år(LocalDate.of(2025, 11, 1));

        var behandling = EndringInntektScenarioer
            .lagBehandlingMedAksjonspunktKontrollerInntekt(ungTestScenario, ungTestRepositories);

        behandling.avsluttBehandling();

        vedtaksbrevValgRepository.lagre(
            new VedtaksbrevValgEntitet(behandling.getId(), DokumentMalType.ENDRING_INNTEKT , true, false, "tekst")
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

        var vedtaksbrevResultater = behandlingVedtaksbrevRepository.hentForBehandling(behandling.getId());
        assertThat(vedtaksbrevResultater).hasSize(1);
        var resultat = vedtaksbrevResultater.getFirst();
        assertThat(resultat.getResultatType()).isEqualTo(VedtaksbrevResultatType.BESTILT);
        assertThat(resultat.getBeskrivelse()).isNotNull();

        var tasker = prosessTaskTjeneste.finnAlle(VedtaksbrevBestillingTask.TASKTYPE, ProsessTaskStatus.KLAR);
        assertThat(tasker).hasSize(1);
        var task = tasker.getFirst();
        assertBestillingTask(task, bestilling);
    }

    @Test
    void skalFeilHvisTomManuellBrevIkkeErRedigert() {
        // Arrange
        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        UngTestScenario ungTestScenario = EndringInntektScenarioer.endring0KrInntekt_19år(LocalDate.of(2025, 11, 1));

        var behandling = EndringInntektScenarioer
            .lagBehandlingMedAksjonspunktKontrollerInntekt(ungTestScenario, ungTestRepositories);
        behandling.avsluttBehandling();

        vedtaksbrevValgRepository.lagre(
            new VedtaksbrevValgEntitet(behandling.getId(), DokumentMalType.MANUELT_VEDTAK_DOK , false, false, "tekst")
        );

        var prosessTaskData = lagTask(behandling);

        // Act
        assertThatThrownBy(() -> task.prosesser(prosessTaskData)).isInstanceOf(IllegalStateException.class).hasMessageContaining("TomVedtaksbrevInnholdBygger");

    }

    @Test
    void skalFeileHvisForsøkerÅBestilleBrevSomIkkeErMulig() {
        // Arrange
        UngTestRepositories ungTestRepositories = BrevTestUtils.lagAlleUngTestRepositories(entityManager);
        UngTestScenario ungTestScenario = EndringInntektScenarioer.endring0KrInntekt_19år(LocalDate.of(2025, 11, 1));

        var behandling = EndringInntektScenarioer
            .lagBehandlingMedAksjonspunktKontrollerInntekt(ungTestScenario, ungTestRepositories);
        behandling.avsluttBehandling();

        vedtaksbrevValgRepository.lagre(
            new VedtaksbrevValgEntitet(behandling.getId(), DokumentMalType.ENDRING_INNTEKT , true, false, "<h1>gammelt brev</h1>")
        );

        vedtaksbrevValgRepository.lagre(
            new VedtaksbrevValgEntitet(behandling.getId(), DokumentMalType.MANUELT_VEDTAK_DOK , true, false, "<h1>Nytt brev</h1>")
        );


        var prosessTaskData = lagTask(behandling);

        // Act
        assertThatThrownBy(() -> task.prosesser(prosessTaskData)).isInstanceOf(IllegalStateException.class).hasMessageContaining("valg");

    }

}
