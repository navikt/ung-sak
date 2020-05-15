package no.nav.k9.sak.metrikker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Rule;
import org.junit.Test;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class StatistikkRepositoryTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    private StatistikkRepository statistikkRepository = new StatistikkRepository(repoRule.getEntityManager());

    @Test
    public void skal_kunne_hente_statistikk() throws Exception {

        assertThat(statistikkRepository.hentAlle()).isNotEmpty().allMatch(v -> v.toString().contains("prosess_task"));
        assertThat(statistikkRepository.hentAlle()).isNotEmpty().allMatch(v -> v.toString().contains("prosess_task"));
    }
    
    @Test
    public void skal_kunne_hente_statistikk_aksjonspunkt() throws Exception {
        var scenario = TestScenarioBuilder.builderUtenSøknad(FagsakYtelseType.FRISINN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_FRISINN_BEREGNING, BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG);
       
        @SuppressWarnings("unused")
        var behandling = scenario.lagre(repoRule.getEntityManager());
        
        assertThat(statistikkRepository.aksjonspunktStatistikk()).isNotEmpty().allMatch(v -> v.toString().contains("aksjonspunkt_per_ytelse_type_v2"));
        assertThat(statistikkRepository.aksjonspunktVenteårsakStatistikk()).isNotEmpty().allMatch(v -> v.toString().contains("aksjonspunkt_ytelse_type_vent_aarsak_v2"));
        
        assertThat(statistikkRepository.aksjonspunktStatistikk()).isNotEmpty().allMatch(v -> v.toString().contains("aksjonspunkt_per_ytelse_type_v2"));
        assertThat(statistikkRepository.aksjonspunktVenteårsakStatistikk()).isNotEmpty().allMatch(v -> v.toString().contains("aksjonspunkt_ytelse_type_vent_aarsak_v2"));
        
        assertThat(statistikkRepository.aksjonspunktStatistikk()).isEmpty();
        assertThat(statistikkRepository.aksjonspunktVenteårsakStatistikk()).isEmpty();
    }

}
