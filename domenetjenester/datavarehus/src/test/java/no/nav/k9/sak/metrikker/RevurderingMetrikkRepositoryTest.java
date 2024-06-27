package no.nav.k9.sak.metrikker;


import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.vilkår.Utfall;
import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatBuilder;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.VilkårResultatRepository;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class RevurderingMetrikkRepositoryTest {

    @Inject
    private EntityManager entityManager;
    private RevurderingMetrikkRepository revurderingMetrikkRepository;
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;
    private VilkårResultatRepository vilkårResultatRepository;

    @BeforeEach
    public void setup() {
        revurderingMetrikkRepository = new RevurderingMetrikkRepository(entityManager);
        aksjonspunktKontrollRepository = new AksjonspunktKontrollRepository();
        vilkårResultatRepository = new VilkårResultatRepository(entityManager);
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


    @Test
    void skal_finne_en_behandling_uten_nytt_stp_med_ett_aksjonspunkt() {

        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var originalBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        var behandling = originalBuilder.lagre(entityManager);
        var stp = LocalDate.now();
        leggTilVilkårResultatForStp(stp, behandling);

        behandling.avsluttBehandling();


        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var revurderingBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        revurderingBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = revurderingBuilder
            .lagre(entityManager);
        leggTilVilkårResultatForStp(stp, revurdering);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallAksjonspunktFordelingForRevurderingUtenNyttStpSisteSyvDager(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_uten_nye_stp_antall_aksjonspunkt_fordeling"))
            .anyMatch(v -> v.toString().contains("ytelse_type=PSB") && v.toString().contains("antall_behandlinger=1") && v.toString().contains("antall_aksjonspunkter=1"));

    }


    @Test
    void skal_finne_en_behandling_uten_ny_søknad_med_ett_aksjonspunkt() {

        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var originalBuilder = TestScenarioBuilder.builderMedSøknad(ytelseType);
        var behandling = originalBuilder.lagre(entityManager);
        var stp = LocalDate.now();
        leggTilVilkårResultatForStp(stp, behandling);

        behandling.avsluttBehandling();


        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var revurderingBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        revurderingBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = revurderingBuilder
            .lagre(entityManager);
        leggTilVilkårResultatForStp(stp, revurdering);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallAksjonspunktFordelingForRevurderingUtenNySøknadSisteSyvDagerPSB(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_uten_ny_soknad_antall_aksjonspunkt_fordeling_v2"))
            .anyMatch(v -> v.toString().contains("ytelse_type=PSB") && v.toString().contains("antall_behandlinger=1") && v.toString().contains("antall_aksjonspunkter=1"));

    }


    @Test
    void skal_ikke_finne_behandling_dersom_nytt_stp() {

        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var originalBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        var behandling = originalBuilder.lagre(entityManager);
        var stp = LocalDate.now();
        leggTilVilkårResultatForStp(stp, behandling);

        behandling.avsluttBehandling();


        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var revurderingBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        revurderingBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = revurderingBuilder
            .lagre(entityManager);
        leggTilVilkårResultatForStp(stp, revurdering);
        var stp2 = LocalDate.now().plusDays(10);
        leggTilVilkårResultatForStp(stp2, revurdering);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallAksjonspunktFordelingForRevurderingUtenNyttStpSisteSyvDager(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_uten_nye_stp_antall_aksjonspunkt_fordeling"))
            .allMatch(v -> v.toString().contains("antall_behandlinger=0"));

    }

    @Test
    void skal_finne_antall_aksjonspunkt_med_endringsopphav() {
        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var scenario = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        var behandling = scenario.lagre(entityManager);
        behandling.avsluttBehandling();

        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var scenarioBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        scenarioBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = scenarioBuilder
            .lagre(entityManager);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallAksjonspunktPrRevurderingMedEndringsopphav(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_antall_aksjonspunkt_pr_behandling_og_endringsopphav"))
            .anyMatch(v -> v.toString().contains("ytelse_type=PSB") &&
                v.toString().contains("behandling_teller=1") &&
                v.toString().contains("behandlinger_prosentandel=100") &&
                v.toString().contains("har_endring_fra_bruker=false") &&
                v.toString().contains("har_endring_fra_inntektsmelding=true") &&
                v.toString().contains("har_endring_fra_annen_sak=false") &&
                v.toString().contains("har_endring_fra_endringsdialog=false")
            );
    }


    @Test
    void skal_finne_antall_revurderinger_pr_aksjonspunkt_og_endringsopphav() {
        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var scenario = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        var behandling = scenario.lagre(entityManager);
        behandling.avsluttBehandling();

        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var scenarioBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);

        scenarioBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = scenarioBuilder
            .lagre(entityManager);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallRevurderingerPrAksjonspunktOgEndringsopphav(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("antall_revurderinger_pr_aksjonspunkt_og_endringsopphav"))
            .anyMatch(v -> v.toString().contains("ytelse_type=PSB") &&
                v.toString().contains("antall_aksjonspunkt_per_behandling=1") &&
                v.toString().contains("har_endring_fra_bruker=false") &&
                v.toString().contains("har_endring_fra_inntektsmelding=true") &&
                v.toString().contains("har_endring_fra_annen_sak=false") &&
                v.toString().contains("har_endring_fra_endringsdialog=false") &&
                v.toString().contains("antall_behandlinger=1")
            );
    }


    @Test
    void skal_finne_aksjonspunkt_med_en_behandling() {

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

        assertThat(revurderingMetrikkRepository.antallRevurderingMedAksjonspunktPrKodeSisteSyvDager(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_antall_behandlinger_pr_aksjonspunkt_v2"))
            .anyMatch(v -> v.toString().contains("ytelse_type=PSB") && v.toString().contains("antall_behandlinger=1") && v.toString().contains("aksjonspunkt=" + aksjonspunkt.getKode()) &&
                v.toString().contains("aksjonspunkt_navn=" + aksjonspunkt.getNavn()));

    }

    @Test
    void skal_finne_aksjonspuknt_med_en_behandling_uten_nytt_stp() {

        var stp = LocalDate.now();

        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var scenario = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        var behandling = scenario.lagre(entityManager);
        leggTilVilkårResultatForStp(stp, behandling);
        behandling.avsluttBehandling();


        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var scenarioBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        scenarioBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = scenarioBuilder
            .lagre(entityManager);
        leggTilVilkårResultatForStp(stp, revurdering);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallRevurderingUtenNyttStpMedAksjonspunktPrKodeSisteSyvDager(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_uten_nytt_stp_antall_behandlinger_pr_aksjonspunkt"))
            .anyMatch(v -> v.toString().contains("ytelse_type=PSB") && v.toString().contains("antall_behandlinger=1") && v.toString().contains("aksjonspunkt=" + aksjonspunkt.getKode()) &&
                v.toString().contains("aksjonspunkt_navn=" + aksjonspunkt.getNavn()));

    }

    @Test
    void skal_finne_aksjonspunkt_med_en_behandling_uten_ny_søknad() {

        var stp = LocalDate.now();

        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var scenario = TestScenarioBuilder.builderMedSøknad(ytelseType);
        var behandling = scenario.lagre(entityManager);
        leggTilVilkårResultatForStp(stp, behandling);
        behandling.avsluttBehandling();


        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var scenarioBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        scenarioBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = scenarioBuilder
            .lagre(entityManager);
        leggTilVilkårResultatForStp(stp, revurdering);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallRevurderingUtenNySøknadMedAksjonspunktPrKodeSisteSyvDagerPSB(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_uten_ny_soknad_antall_behandlinger_pr_aksjonspunkt"))
            .anyMatch(v -> v.toString().contains("ytelse_type=PSB") && v.toString().contains("antall_behandlinger=1") && v.toString().contains("aksjonspunkt=" + aksjonspunkt.getKode()) &&
                v.toString().contains("aksjonspunkt_navn=" + aksjonspunkt.getNavn()));

    }


    @Test
    void skal_finne_en_sak_uten_ny_søknad() {

        var stp = LocalDate.now();

        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var scenario = TestScenarioBuilder.builderMedSøknad(ytelseType);
        var behandling = scenario.lagre(entityManager);
        leggTilVilkårResultatForStp(stp, behandling);
        behandling.avsluttBehandling();


        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var scenarioBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        scenarioBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = scenarioBuilder
            .lagre(entityManager);
        leggTilVilkårResultatForStp(stp, revurdering);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.revurderingerUtenNySøknadMedAksjonspunkt(LocalDate.now().plusDays(1))).isNotEmpty()
            .allMatch(v -> v.toString().contains("revurdering_uten_ny_soknad"))
            .anyMatch(v -> v.toString().contains("ytelse_type=PSB") && v.toString().contains("saksnummer=" + revurdering.getFagsak().getSaksnummer()) && v.toString().contains("aksjonspunkt=" + aksjonspunkt.getKode()) &&
                v.toString().contains("aksjonspunkt_navn=" + aksjonspunkt.getNavn()));

    }

    @Test
    void skal_ikke_finne_aksjonspuknt_for_behandling_med_nytt_stp() {

        var stp = LocalDate.now();

        FagsakYtelseType ytelseType = FagsakYtelseType.PSB;
        var scenario = TestScenarioBuilder.builderUtenSøknad(ytelseType);
        var behandling = scenario.lagre(entityManager);
        leggTilVilkårResultatForStp(stp, behandling);
        behandling.avsluttBehandling();


        AksjonspunktDefinisjon aksjonspunkt = AksjonspunktDefinisjon.FASTSETT_BEREGNINGSGRUNNLAG_SELVSTENDIG_NÆRINGSDRIVENDE;
        BehandlingStegType stegType = BehandlingStegType.FORESLÅ_BEREGNINGSGRUNNLAG;

        var scenarioBuilder = TestScenarioBuilder.builderUtenSøknad(ytelseType)
            .medBehandlingType(BehandlingType.REVURDERING)
            .medOriginalBehandling(behandling, BehandlingÅrsakType.RE_ENDRING_BEREGNINGSGRUNNLAG);

        scenarioBuilder.leggTilAksjonspunkt(aksjonspunkt, stegType);

        var revurdering = scenarioBuilder
            .lagre(entityManager);
        leggTilVilkårResultatForStp(stp, revurdering);
        var stp2 = stp.plusDays(10);
        leggTilVilkårResultatForStp(stp2, revurdering);

        var ap = revurdering.getAksjonspunkter().iterator().next();
        aksjonspunktKontrollRepository.setTilUtført(ap, "begrunnelse");

        revurdering.avsluttBehandling();

        entityManager.flush();

        assertThat(revurderingMetrikkRepository.antallRevurderingUtenNyttStpMedAksjonspunktPrKodeSisteSyvDager(LocalDate.now().plusDays(1))).isEmpty();

    }

    private void leggTilVilkårResultatForStp(LocalDate stp, Behandling behandling) {
        var vilkårResultatBuilder = new VilkårResultatBuilder();
        var vilkårBuilder = vilkårResultatBuilder.hentBuilderFor(VilkårType.BEREGNINGSGRUNNLAGVILKÅR);
        var vilkårPeriodeBuilder = vilkårBuilder.hentBuilderFor(stp, stp.plusDays(1));
        vilkårPeriodeBuilder.medUtfall(Utfall.OPPFYLT);
        vilkårBuilder.leggTil(vilkårPeriodeBuilder);
        vilkårResultatBuilder.leggTil(vilkårBuilder);
        var vilkårene = vilkårResultatBuilder.build();
        vilkårResultatRepository.lagre(behandling.getId(), vilkårene);
    }


}
