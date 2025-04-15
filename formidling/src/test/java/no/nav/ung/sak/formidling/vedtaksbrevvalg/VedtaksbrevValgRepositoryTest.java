package no.nav.ung.sak.formidling.vedtaksbrevvalg;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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
            true,
            false,
            "<h1>Et redigert brev</h1>"
        );

        vedtaksbrevValgRepository.lagre(vedtaksbrevValgEntitet);
        entityManager.flush();

        VedtaksbrevValgEntitet resultat = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId());

        assertThat(resultat.getId()).isNotNull();
        assertThat(resultat.getBehandlingId()).isEqualTo(behandling.getId());
        assertThat(resultat.isRedigert()).isTrue();
        assertThat(resultat.isHindret()).isFalse();
        assertThat(resultat.getRedigertBrevHtml()).isEqualTo("<h1>Et redigert brev</h1>");
        assertThat(resultat.getVersjon()).isEqualTo(0);

    }

    @Test
    void skalFeileHvisEndrerPåUtdatertVersjon() {
        TestScenarioBuilder scenarioBuilder = TestScenarioBuilder.builderMedSøknad();
        scenarioBuilder.lagre(repositoryProvider);
        var behandling = scenarioBuilder.getBehandling();

        var original = new VedtaksbrevValgEntitet(
            behandling.getId(),
            false,
            false,
            null
        );

        original = vedtaksbrevValgRepository.lagre(original);
        entityManager.flush();
        entityManager.detach(original);
        assertThat(original.getVersjon()).isEqualTo(0);

        //klient 1 endrer før lagring
        VedtaksbrevValgEntitet kopi1 = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId());
        kopi1.redigerTekst("Vil feile");
        entityManager.detach(kopi1);
        assertThat(kopi1.getId()).isEqualTo(original.getId());
        assertThat(kopi1.getVersjon()).isEqualTo(0);

        //klient 2 endrer og rekker å lagre
        VedtaksbrevValgEntitet kopi2 = vedtaksbrevValgRepository.finnVedtakbrevValg(behandling.getId());
        kopi2.redigerTekst("Vil gå bra");
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
