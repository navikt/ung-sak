package no.nav.k9.sak.test.util.behandling;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.test.util.fagsak.FagsakBuilder;
import no.nav.k9.sak.typer.Saksnummer;

public class BehandlingLåsTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private EntityManager em = repoRule.getEntityManager();
    private final BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(em);
    private final Saksnummer saksnummer  = new Saksnummer("2");

    private Fagsak fagsak;

    private Behandling behandling;

    @Before
    public void setup() {
        fagsak = FagsakBuilder.nyFagsak(FagsakYtelseType.OMSORGSPENGER).medSaksnummer(saksnummer).build();
        em.persist(fagsak);
        em.flush();

        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        em.persist(behandling);
        em.flush();
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
