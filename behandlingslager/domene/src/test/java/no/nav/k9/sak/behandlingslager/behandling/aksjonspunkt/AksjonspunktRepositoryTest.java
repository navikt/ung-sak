package no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

public class AksjonspunktRepositoryTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();

    private EntityManager em = repoRule.getEntityManager();
    private AksjonspunktRepository aksjonspunktRepository = new AksjonspunktRepository(em);
    private BehandlingRepository behandlingRepository = new BehandlingRepository(em);
    private FagsakRepository fagsakRepository = new FagsakRepository(em);

    private Fagsak fagsak;
    private Behandling behandling;

    @Before
    public void setup() {
        fagsak = Fagsak.opprettNy(FagsakYtelseType.DAGPENGER, new AktørId(123L), new Saksnummer("987"));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    public void hent_aksjonspunkt_for_behandling() throws Exception {
        var aksjonspunkter0 = opprettAksjonspunkt(AUTO_MANUELT_SATT_PÅ_VENT);
        var aksjonspunkter = aksjonspunktRepository.hentAksjonspunkter(behandling.getId(), AksjonspunktStatus.OPPRETTET);
        assertThat(aksjonspunkter).containsExactlyInAnyOrderElementsOf(aksjonspunkter0);

    }
    
    @Test
    public void hent_aksjonspunkt_for_saksnummer() throws Exception {
        var aksjonspunkter0 = opprettAksjonspunkt(AUTO_MANUELT_SATT_PÅ_VENT);
        var aksjonspunkterPerBehandling = aksjonspunktRepository.hentAksjonspunkter(fagsak.getSaksnummer(), AksjonspunktStatus.OPPRETTET);
        assertThat(aksjonspunkterPerBehandling).containsKey(behandling).hasSize(1);
        assertThat(aksjonspunkterPerBehandling.values()).containsOnly(aksjonspunkter0);

    }

    @Test
    public void hent_aksjonspunkt_alle_fagsaker() throws Exception {
        var aksjonspunkter0 = opprettAksjonspunkt(AUTO_MANUELT_SATT_PÅ_VENT);
        var aksjonspunkterPerBehandling = aksjonspunktRepository.hentAksjonspunkter(AksjonspunktStatus.OPPRETTET);
        assertThat(aksjonspunkterPerBehandling).containsKey(behandling).hasSize(1);
        assertThat(aksjonspunkterPerBehandling.values()).containsOnly(aksjonspunkter0);

    }

    private List<Aksjonspunkt> opprettAksjonspunkt(AksjonspunktDefinisjon... aksjonspunktDef) {
        List<Aksjonspunkt> lst = new ArrayList<>();
        for (var a : aksjonspunktDef) {
            lst.add(aksjonspunktTestSupport.leggTilAksjonspunkt(behandling, a));
        }
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return lst;
    }
}
