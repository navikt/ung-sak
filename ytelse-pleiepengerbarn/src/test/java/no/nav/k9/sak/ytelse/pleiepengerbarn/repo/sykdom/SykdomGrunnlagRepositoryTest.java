package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.sykdom;

import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class SykdomGrunnlagRepositoryTest {
    @Inject
    private SykdomGrunnlagRepository repo;

    @Test
    void sjekkGyldigHqlSyntax() {
        repo.hentSisteBehandlingMedUnntakAv(new Saksnummer("A21A"), UUID.randomUUID());
        repo.hentGrunnlagForBehandling(UUID.randomUUID());
    }
}
