package no.nav.ung.sak.formidling.vedtaksbrevvalg;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
}
