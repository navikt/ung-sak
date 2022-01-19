package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.infotrygd;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class PsbInfotrygdRepositoryTest {

    @Inject
    private PsbInfotrygdRepository psbInfotrygdRepository;

    
    @Test
    void lagreOgLesOppIgjen() {
        final AktørId p1 = new AktørId("1");
        final AktørId p2 = new AktørId("2");
        assertThat(psbInfotrygdRepository.finnes(p1)).isFalse();
        assertThat(psbInfotrygdRepository.finnes(p2)).isFalse();
        psbInfotrygdRepository.lagre(p1);
        assertThat(psbInfotrygdRepository.finnes(p1)).isTrue();
        assertThat(psbInfotrygdRepository.finnes(p2)).isFalse();
    }
}
