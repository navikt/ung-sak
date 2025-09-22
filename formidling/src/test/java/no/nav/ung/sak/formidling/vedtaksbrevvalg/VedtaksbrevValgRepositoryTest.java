package no.nav.ung.sak.formidling.vedtaksbrevvalg;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.dokument.DokumentMalType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgEntitet;
import no.nav.ung.sak.behandlingslager.formidling.VedtaksbrevValgRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class VedtaksbrevValgRepositoryTest {

    @Inject
    private EntityManager entityManager;
    private VedtaksbrevValgRepository vedtaksbrevValgRepository;
    private BehandlingRepositoryProvider repositoryProvider;


    @BeforeEach
    void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        vedtaksbrevValgRepository = new VedtaksbrevValgRepository(entityManager);
    }


    @Test
    void skalLagreOgHente() {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        scenarioBuilder.lagre(repositoryProvider);
        var behandling = scenarioBuilder.getBehandling();

        var vedtaksbrevValgEntitet = new VedtaksbrevValgEntitet(
            behandling.getId(),
            DokumentMalType.ENDRING_INNTEKT,
            true,
            false,
            "<h1>Et redigert brev</h1>");

        vedtaksbrevValgRepository.lagre(vedtaksbrevValgEntitet);
        entityManager.flush();

        VedtaksbrevValgEntitet resultat = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId(), DokumentMalType.ENDRING_INNTEKT).get();

        assertThat(resultat.getId()).isNotNull();
        assertThat(resultat.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(resultat.isRedigert()).isTrue();
        assertThat(resultat.isHindret()).isFalse();
        assertThat(resultat.getRedigertBrevHtml()).isEqualTo("<h1>Et redigert brev</h1>");
        assertThat(resultat.getVersjon()).isEqualTo(0);

    }

    @Test
    void skalHenteNyesteDeaktiverteValg() {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        scenarioBuilder.lagre(repositoryProvider);
        var behandling = scenarioBuilder.getBehandling();

        var v1 = new VedtaksbrevValgEntitet(
            behandling.getId(),
            DokumentMalType.ENDRING_INNTEKT,
            true,
            false,
            "første");

        var v2 = new VedtaksbrevValgEntitet(
            behandling.getId(),
            DokumentMalType.MANUELT_VEDTAK_DOK,
            true,
            false,
            "<h1>Et annet brev</h1>");

        var v3 = new VedtaksbrevValgEntitet(
            behandling.getId(),
            DokumentMalType.ENDRING_INNTEKT,
            true,
            false,
            "andre");

        var v4 = new VedtaksbrevValgEntitet(
            behandling.getId(),
            DokumentMalType.ENDRING_INNTEKT,
            true,
            false,
            "tredje");


        v1.deaktiver();
        v2.deaktiver();
        v3.deaktiver();
        v4.deaktiver();

        vedtaksbrevValgRepository.lagre(v1);
        vedtaksbrevValgRepository.lagre(v2);
        vedtaksbrevValgRepository.lagre(v3);
        vedtaksbrevValgRepository.lagre(v4);
        entityManager.flush();

        var resultat = vedtaksbrevValgRepository.finnNyesteDeaktiverteVedtakbrevValg(behandling.getId());

        assertThat(resultat).hasSize(2);
        var manueltBrev = resultat.stream().filter(v -> v.getDokumentMalType() == DokumentMalType.MANUELT_VEDTAK_DOK).findFirst().orElseThrow();
        assertThat(manueltBrev.getRedigertBrevHtml()).isEqualTo("<h1>Et annet brev</h1>");

        var endringInntektBrev = resultat.stream().filter(v -> v.getDokumentMalType() == DokumentMalType.ENDRING_INNTEKT).findFirst().orElseThrow();
        assertThat(endringInntektBrev.getRedigertBrevHtml()).isEqualTo("tredje");
    }

    @Test
    void skalFeileHvisEndrerPåUtdatertVersjon() {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        scenarioBuilder.lagre(repositoryProvider);
        var behandling = scenarioBuilder.getBehandling();

        var original = new VedtaksbrevValgEntitet(
            behandling.getId(),
            DokumentMalType.ENDRING_INNTEKT,
            false,
            false,
            null);

        original = vedtaksbrevValgRepository.lagre(original);
        entityManager.flush();
        entityManager.detach(original);
        assertThat(original.getVersjon()).isEqualTo(0);

        //klient 1 endrer før lagring
        VedtaksbrevValgEntitet kopi1 = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId(), DokumentMalType.ENDRING_INNTEKT).get();
        kopi1.rensOgSettRedigertHtml("Vil feile");
        kopi1.setRedigert(true);
        entityManager.detach(kopi1);
        assertThat(kopi1.getId()).isEqualTo(original.getId());
        assertThat(kopi1.getVersjon()).isEqualTo(0);

        //klient 2 endrer og rekker å lagre
        VedtaksbrevValgEntitet kopi2 = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId(), DokumentMalType.ENDRING_INNTEKT).get();
        kopi2.rensOgSettRedigertHtml("Vil gå bra");
        kopi2.setRedigert(true);
        kopi2 = vedtaksbrevValgRepository.lagre(kopi2);
        entityManager.flush();
        entityManager.detach(kopi2);
        assertThat(kopi1.getId()).isEqualTo(kopi2.getId());
        assertThat(kopi2.getVersjon()).isEqualTo(1);


        //klient 1 forøsker å lagre
        assertThatThrownBy(() -> {
            entityManager.merge(kopi1);
            entityManager.flush();
        }).isInstanceOf(OptimisticLockException.class);


    }

}
