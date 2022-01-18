package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.pleietrengende.død;

import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.uttak.RettVedDødType;
import no.nav.k9.sak.db.util.JpaExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class RettPleiepengerVedDødRepositoryTest {

    @Inject
    private RettPleiepengerVedDødRepository repo;

    @Test
    void lagre_og_hent_igjen() {
        var behandlingId = 123L;

        var rettVedDød = new RettPleiepengerVedDød("Har rett på 6 uker.", RettVedDødType.RETT_6_UKER);
        repo.lagreOgFlush(behandlingId, rettVedDød);


        var grunnlag = repo.hentHvisEksisterer(behandlingId);


        assertThat(grunnlag.isPresent()).isTrue();
        assertThat(grunnlag.get().getRettVedPleietrengendeDød()).isNotNull();
        assertThat(grunnlag.get().getRettVedPleietrengendeDød().getRettVedDødType()).isEqualTo(RettVedDødType.RETT_6_UKER);
        assertThat(grunnlag.get().getRettVedPleietrengendeDød().getVurdering()).isEqualTo("Har rett på 6 uker.");
    }
}
