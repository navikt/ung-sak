package no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class AksjonspunktRepositoryTest {

    private AksjonspunktTestSupport aksjonspunktTestSupport = new AksjonspunktTestSupport();

    @Inject
    private EntityManager entityManager;

    private AksjonspunktRepository aksjonspunktRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;

    private Fagsak fagsak;
    private Behandling behandling;

    @BeforeEach
    public void setup() {

        aksjonspunktRepository = new AksjonspunktRepository(entityManager);
        behandlingRepository = new BehandlingRepository(entityManager);
        fagsakRepository = new FagsakRepository(entityManager);

        fagsak = Fagsak.opprettNy(FagsakYtelseType.DAGPENGER, new AktørId(123L), new Saksnummer("987"));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).medBehandlingStatus(BehandlingStatus.UTREDES).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
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

    @Test
    public void hent_bruker_ident_for_aksjonspunkt() throws Exception {
        @SuppressWarnings("unused")
        var aksjonspunkter0 = opprettAksjonspunkt(AUTO_MANUELT_SATT_PÅ_VENT);
        var aksjonspunkterPerBehandling = aksjonspunktRepository.hentAktørerMedAktivtAksjonspunkt(AUTO_MANUELT_SATT_PÅ_VENT);
        var bruker = behandling.getAktørId();
        assertThat(aksjonspunkterPerBehandling).contains(bruker).hasSize(1);

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
