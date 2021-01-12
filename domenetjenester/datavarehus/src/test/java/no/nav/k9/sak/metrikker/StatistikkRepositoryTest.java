package no.nav.k9.sak.metrikker;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.CdiAwareExtension;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class StatistikkRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private StatistikkRepository statistikkRepository ;

    @BeforeEach
    public void setup(){
        statistikkRepository = new StatistikkRepository(entityManager);
    }


    @Test
    public void skal_kunne_hente_statistikk() throws Exception {

        assertThat(statistikkRepository.prosessTaskStatistikk()).isNotEmpty().allMatch(v -> v.toString().contains("prosess_task_" + StatistikkRepository.PROSESS_TASK_VER));

        assertThat(statistikkRepository.behandlingResultatStatistikk()).isNotEmpty().allMatch(v -> v.toString().contains("behandling_resultat_v1"));

        assertThat(statistikkRepository.hentAlle()).isNotEmpty()
            .anyMatch(v -> v.toString().contains("mottatt_dokument_v1"))
            .anyMatch(v -> v.toString().contains("behandling_resultat_v1"))
            .anyMatch(v -> v.toString().contains("behandling_status_v2"))
            .anyMatch(v -> v.toString().contains("fagsak_status_v2"))
            .anyMatch(v -> v.toString().contains("aksjonspunkt_per_ytelse_type_v3"))
            .anyMatch(v -> v.toString().contains("prosess_task_" + StatistikkRepository.PROSESS_TASK_VER));

    }

    @Test
    public void skal_kunne_hente_statistikk_aksjonspunkt() throws Exception {
        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.AUTO_VENT_FRISINN_BEREGNING;
        BehandlingStegType stegType = BehandlingStegType.FASTSETT_BEREGNINGSGRUNNLAG;
        FagsakYtelseType ytelseType = FagsakYtelseType.FRISINN;

        var scenario = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        scenario.leggTilAksjonspunkt(aksjonspunkt, stegType);

        @SuppressWarnings("unused")
        var behandling = scenario.lagre(entityManager);

        assertThat(statistikkRepository.aksjonspunktStatistikk()).isNotEmpty()
            .allMatch(v -> v.toString().contains("aksjonspunkt_per_ytelse_type_v3"))
            .anyMatch(v -> v.toString().contains("totalt_antall=0"))
            .anyMatch(v -> v.toString().contains("ytelse_type=" + ytelseType.getKode()) && v.toString().contains("aksjonspunkt=" + aksjonspunkt.getKode()) && v.toString().contains("totalt_antall=1"));

    }

}
