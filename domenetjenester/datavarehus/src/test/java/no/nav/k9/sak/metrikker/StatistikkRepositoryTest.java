package no.nav.k9.sak.metrikker;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

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

        assertThat(statistikkRepository.hentAlle(null)).isNotEmpty().allMatch(v -> v.toString().contains("prosess_task"));
        assertThat(statistikkRepository.hentAlle(LocalDate.now())).isNotEmpty().allMatch(v -> v.toString().contains("prosess_task"));
    }
    
    @Test
    public void skal_kunne_hente_statistikk_aksjonspunkt() throws Exception {
        var scenario = TestScenarioBuilder.builderUtenSøknad(FagsakYtelseType.FRISINN);
        scenario.leggTilAksjonspunkt(AksjonspunktDefinisjon.AUTO_VENT_FRISINN_BEREGNING, BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG);
       
        @SuppressWarnings("unused")
        var behandling = scenario.lagre(repoRule.getEntityManager());
        
        assertThat(statistikkRepository.aksjonspunktStatistikk(LocalDate.now())).isNotEmpty().allMatch(v -> v.toString().contains("aksjonspunkt_per_ytelse_type"));
        assertThat(statistikkRepository.aksjonspunktVenteårsakStatistikk(LocalDate.now())).isNotEmpty().allMatch(v -> v.toString().contains("aksjonspunkt_ytelse_type_vent_aarsak"));
        
        assertThat(statistikkRepository.aksjonspunktStatistikk(null)).isNotEmpty().allMatch(v -> v.toString().contains("aksjonspunkt_per_ytelse_type"));
        assertThat(statistikkRepository.aksjonspunktVenteårsakStatistikk(null)).isNotEmpty().allMatch(v -> v.toString().contains("aksjonspunkt_ytelse_type_vent_aarsak"));
        
        assertThat(statistikkRepository.aksjonspunktStatistikk(LocalDate.now().minusDays(1))).isEmpty();
        assertThat(statistikkRepository.aksjonspunktVenteårsakStatistikk(LocalDate.now().minusDays(1))).isEmpty();
    }

}
