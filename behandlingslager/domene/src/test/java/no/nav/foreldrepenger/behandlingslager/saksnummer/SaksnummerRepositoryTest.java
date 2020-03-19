package no.nav.foreldrepenger.behandlingslager.saksnummer;

import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.sak.db.util.UnittestRepositoryRule;

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
