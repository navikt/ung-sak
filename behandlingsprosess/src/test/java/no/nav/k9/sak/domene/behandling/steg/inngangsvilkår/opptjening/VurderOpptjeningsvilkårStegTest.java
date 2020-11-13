package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class VurderOpptjeningsvilkårStegTest {

    @Inject
    private EntityManager entityManager;

    @Inject
    public InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;

    @Inject
    private OpptjeningRepository opptjeningRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

    private BehandlingRepositoryProvider repositoryProvider ;

    @BeforeEach
    public void setup(){
        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(repositoryProvider);
    }

    @Test
    public void skal_lagre_resultat_av_opptjeningsvilkår() throws Exception {

        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSPERIODEVILKÅR, Utfall.IKKE_VURDERT);
        scenario.leggTilVilkår(VilkårType.OPPTJENINGSVILKÅRET, Utfall.IKKE_VURDERT);

        Behandling behandling = lagre(scenario);
        Fagsak fagsak = behandling.getFagsak();
        BehandlingskontrollKontekst kontekst = new BehandlingskontrollKontekst(fagsak.getId(), fagsak.getAktørId(),
            behandlingRepository.taSkriveLås(behandling));

        // Act
        // opprett opptjening
        new FastsettOpptjeningsperiodeSteg(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste)
            .utførSteg(kontekst);

        // vurder vilkåret
        new VurderOpptjeningsvilkårSteg(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste, new UnitTestLookupInstanceImpl<>(new DefaultHåndtereAutomatiskAvslag()))
            .utførSteg(kontekst);
    }
}
