package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.opptjening.OpptjeningRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriodeBuilder;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.InngangsvilkårFellesTjeneste;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.vedtak.felles.testutilities.db.RepositoryRule;

@RunWith(CdiRunner.class)
public class VurderOpptjeningsvilkårStegTest {

    @Rule
    public final RepositoryRule repoRule = new UnittestRepositoryRule();
    @Inject
    public InngangsvilkårFellesTjeneste inngangsvilkårFellesTjeneste;
    private BehandlingRepositoryProvider repositoryProvider = new BehandlingRepositoryProvider(repoRule.getEntityManager());
    @Inject
    private OpptjeningRepository opptjeningRepository;

    @Inject
    private BehandlingRepository behandlingRepository;

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

    @Test
    public void skal_gi_steg_utført_ved_ikke_alle_avslått() {
        var fagsak = new Fagsak(FagsakYtelseType.PSB, AktørId.dummy(), new Saksnummer("1234"));
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var steg = new VurderOpptjeningsvilkårSteg(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste, new UnitTestLookupInstanceImpl<>(new DefaultHåndtereAutomatiskAvslag()));
        var vilkåret = new VilkårBuilder()
            .medType(VilkårType.OPPTJENINGSVILKÅRET)
            .build();

        var stegResultat = steg.utledStegResultat(behandling, BehandleStegResultat.utførtUtenAksjonspunkter(), vilkåret, List.of());

        assertThat(stegResultat).isNotNull();
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.UTFØRT);
    }

    @Test
    public void skal_utlede_fremoverhopp_ved_alle_perioder_utledet_til_avslag() {
        var fagsak = new Fagsak(FagsakYtelseType.PSB, AktørId.dummy(), new Saksnummer("1234"));
        var behandling = Behandling.forFørstegangssøknad(fagsak).build();
        var periode = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusDays(30), LocalDate.now());
        var steg = new VurderOpptjeningsvilkårSteg(repositoryProvider, opptjeningRepository, inngangsvilkårFellesTjeneste, new UnitTestLookupInstanceImpl<>(new DefaultHåndtereAutomatiskAvslag()));
        var vilkåret = new VilkårBuilder()
            .medType(VilkårType.OPPTJENINGSVILKÅRET)
            .leggTil(new VilkårPeriodeBuilder()
                .medPeriode(LocalDate.now().minusDays(30), LocalDate.now())
                .medUtfall(Utfall.IKKE_OPPFYLT))
            .build();

        var stegResultat = steg.utledStegResultat(behandling, BehandleStegResultat.utførtUtenAksjonspunkter(), vilkåret, List.of(periode));

        assertThat(stegResultat).isNotNull();
        assertThat(stegResultat.getTransisjon()).isEqualTo(FellesTransisjoner.FREMHOPP_TIL_FORESLÅ_BEHANDLINGSRESULTAT);
    }
}
