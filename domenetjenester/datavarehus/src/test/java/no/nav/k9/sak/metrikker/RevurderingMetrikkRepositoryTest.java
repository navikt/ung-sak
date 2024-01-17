package no.nav.k9.sak.metrikker;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class RevurderingMetrikkRepositoryTest {

    @Inject
    private EntityManager entityManager;

    private RevurderingMetrikkRepository revurderingMetrikkRepository;

    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    @BeforeEach
    public void setup() {
        revurderingMetrikkRepository = new RevurderingMetrikkRepository(entityManager);
        aksjonspunktKontrollRepository = new AksjonspunktKontrollRepository();
    }

    @Test
    void skal_ikke_finne_førstegangsbehandling() {

        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;
        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;

        var scenario = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        scenario.leggTilAksjonspunkt(aksjonspunkt, stegType);


        @SuppressWarnings("unused")
        var behandling = scenario.lagre(entityManager);

        var ap = behandling.getAksjonspunkter().iterator().next();

        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        behandling.avsluttBehandling();

        entityManager.flush();


        assertThat(revurderingMetrikkRepository.antallAksjonspunktFordelingForRevurderingSisteSyvDager(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_antall_aksjonspunkt_fordeling_v2"))
            .allMatch(v -> v.toString().contains("antall_behandlinger=0"));

    }


    @Test
    void skal_ikke_inkluderer_ikke_avsluttet_behandling() {
        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var scenario = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        var behandling = scenario.lagre(entityManager);
        behandling.avsluttBehandling();


        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var scenarioBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        scenarioBuilder
            .leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = scenarioBuilder
            .lagre(entityManager);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallAksjonspunktFordelingForRevurderingSisteSyvDager(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_antall_aksjonspunkt_fordeling_v2"))
            .allMatch(v -> v.toString().contains("antall_behandlinger=0"));

    }


    @Test
    void skal_finne_en_behandling_med_ett_aksjonspunkt() {

        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var scenario = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        var behandling = scenario.lagre(entityManager);
        behandling.avsluttBehandling();


        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var scenarioBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        scenarioBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = scenarioBuilder
            .lagre(entityManager);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallAksjonspunktFordelingForRevurderingSisteSyvDager(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_antall_aksjonspunkt_fordeling_v2"))
            .anyMatch(v -> v.toString().contains("ytelse_type=PSB") && v.toString().contains("antall_behandlinger=1") && v.toString().contains("antall_aksjonspunkter=1"));

    }
}
