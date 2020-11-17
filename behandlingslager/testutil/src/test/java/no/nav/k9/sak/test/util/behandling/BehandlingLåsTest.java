package no.nav.k9.sak.test.util.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.fagsak.FagsakBuilder;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class BehandlingLåsTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;

    private Saksnummer saksnummer  = new Saksnummer("2");

    private Fagsak fagsak;

    private Behandling behandling;

    @BeforeEach
    public void setup() {
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);

        fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).medSaksnummer(saksnummer).build();
        entityManager.persist(fagsak);
        entityManager.flush();

        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        entityManager.persist(behandling);
        entityManager.flush();
    }

    @Test
    public void skal_finne_behandling_gitt_id() {

        // Act
        BehandlingLås lås = repositoryProvider.getBehandlingRepository().taSkriveLås(behandling);
        assertThat(lås).isNotNull();

        Behandling resultat = repositoryProvider.getBehandlingRepository().hentBehandling(behandling.getId());
        assertThat(resultat).isNotNull();

        // Assert

        repositoryProvider.getBehandlingRepository().lagre(resultat, lås);
    }

}
