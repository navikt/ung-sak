package no.nav.foreldrepenger.behandlingslager.saksnummer;

import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class SaksnummerRepositoryTest {

    @Rule
    public UnittestRepositoryRule repositoryRule = new UnittestRepositoryRule();
    private SaksnummerRepository saksnummerRepository = new SaksnummerRepository(repositoryRule.getEntityManager());

    @Test
    public void skalGenerereNyeSaksnummereVedKall() {
        assertThat(saksnummerRepository.genererNyttSaksnummer()).isNotBlank();
        assertThat(saksnummerRepository.genererNyttSaksnummer()).isNotEqualTo(saksnummerRepository.genererNyttSaksnummer());
    }
}
