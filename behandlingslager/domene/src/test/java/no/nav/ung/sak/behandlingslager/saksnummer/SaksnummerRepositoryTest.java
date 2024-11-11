package no.nav.ung.sak.behandlingslager.saksnummer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class SaksnummerRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private SaksnummerRepository saksnummerRepository;

    @BeforeEach
    public void setup() {
        saksnummerRepository = new SaksnummerRepository(entityManager);
    }

    @Test
    public void skalGenerereNyeSaksnummereVedKall() {
        assertThat(saksnummerRepository.genererNyttSaksnummer()).isNotBlank();
        assertThat(saksnummerRepository.genererNyttSaksnummer()).isNotEqualTo(saksnummerRepository.genererNyttSaksnummer());
    }
}
