package no.nav.ung.sak.metrikker;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.prosesstask.api.ProsessTaskHandler;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class StatistikkRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private StatistikkRepository statistikkRepository;

    @Inject
    private @Any Instance<ProsessTaskHandler> handlers;

    @BeforeEach
    void setup(){
        statistikkRepository = new StatistikkRepository(entityManager, handlers);
    }


    @Test
    void skal_kunne_hente_statistikk()  {

        assertThat(statistikkRepository.prosessTaskStatistikk()).isNotEmpty().allMatch(v -> v.toString().contains("prosess_task_" + StatistikkRepository.PROSESS_TASK_VER));

        assertThat(statistikkRepository.behandlingResultatStatistikk()).isNotEmpty().allMatch(v -> v.toString().contains("behandling_resultat_v1"));

        assertThat(statistikkRepository.hentHyppigRapporterte()).isNotEmpty()
            .anyMatch(v -> v.toString().contains("mottatt_dokument_v1"))
            .anyMatch(v -> v.toString().contains("mottatt_dokument_med_kilde_v1"))
            .anyMatch(v -> v.toString().contains("behandling_resultat_v1"))
            .anyMatch(v -> v.toString().contains("behandling_status_v2"))
            .anyMatch(v -> v.toString().contains("fagsak_status_v2"))
            .anyMatch(v -> v.toString().contains("aksjonspunkt_per_ytelse_type_v3"))
            .anyMatch(v -> v.toString().contains("prosess_task_" + StatistikkRepository.PROSESS_TASK_VER))
            .noneMatch(v -> v.toString().contains("avslagStatistikk"));
    }

    @Test
    void skal_kunne_hente_statistikk_aksjonspunkt()  {
        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.AUTO_MANUELT_SATT_PÅ_VENT;
        BehandlingStegType stegType = BehandlingStegType.BEREGN_YTELSE;
        FagsakYtelseType ytelseType = FagsakYtelseType.UNGDOMSYTELSE;

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
