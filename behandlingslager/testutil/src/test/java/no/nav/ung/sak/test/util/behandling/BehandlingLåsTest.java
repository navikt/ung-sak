package no.nav.ung.sak.test.util.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.fagsak.FagsakBuilder;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;

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
