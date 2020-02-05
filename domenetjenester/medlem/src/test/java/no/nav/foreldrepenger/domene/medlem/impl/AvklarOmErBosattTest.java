package no.nav.foreldrepenger.domene.medlem.impl;

import static java.util.Collections.singletonList;
import static no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat.AVKLAR_OM_ER_BOSATT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonAdresse;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personas;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AvklarOmErBosattTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private MedlemskapRepository medlemskapRepository;
    
    @Inject
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    private AvklarOmErBosatt avklarOmErBosatt;

    @Before
    public void setUp() {
        this.avklarOmErBosatt = new AvklarOmErBosatt(medlemskapRepository, medlemskapPerioderTjeneste, personopplysningTjeneste);
    }

    @Test
    public void skal_gi_medlem_resultat_AVKLAR_OM_ER_BOSATT() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.SWE);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_ikke_gi_medlem_resultat_AVKLAR_OM_ER_BOSATT() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medDefaultOppgittTilknytning();

        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_når_bruker_har_utenlandsk_postadresse_og_dekningsgraden_er_frivillig_medlem() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, Landkoder.USA);
        LocalDate fødselsDato = LocalDate.now();
        MedlemskapPerioderEntitet gyldigPeriodeUnderFødsel = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_9_2_a) // hjemlet i bokstav a
            .medMedlemskapType(MedlemskapType.ENDELIG) // gyldig
            .medPeriode(LocalDate.now(), LocalDate.now())
            .build();

        scenario.leggTilMedlemskapPeriode(gyldigPeriodeUnderFødsel);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_få_aksjonspunkt_når_bruker_har_utenlandsk_postadresse_og_dekningsgraden_er_ikke_medlem() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.MIDLERTIDIG_POSTADRESSE_UTLAND, Landkoder.USA);
        LocalDate fødselsDato = LocalDate.now();
        MedlemskapPerioderEntitet gyldigPeriodeUnderFødsel = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_6) // hjemlet i bokstav a
            .medMedlemskapType(MedlemskapType.ENDELIG) // gyldig
            .medPeriode(LocalDate.now(), LocalDate.now())
            .build();

        scenario.leggTilMedlemskapPeriode(gyldigPeriodeUnderFødsel);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_dersom_minst_to_av_spørsmål_til_bruker_om_tilknytning_er_nei() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        MedlemskapOppgittLandOppholdEntitet oppholdUtlandForrigePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .erTidligereOpphold(true)
            .medLand(Landkoder.BEL)
            .medPeriode(LocalDate.now(), LocalDate.now().plusYears(1))
            .build();
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        scenario.medOppgittTilknytning().medOpphold(singletonList(oppholdUtlandForrigePeriode)).medOppholdNå(false);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_bare_ett_av_spørsmål_til_bruker_om_tilknytning_er_nei() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).isEmpty();
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario, AdresseType adresseType, Landkoder adresseLand) {
        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        Personas persona = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.UOPPGITT, Region.UDEFINERT)
            .personstatus(PersonstatusType.UDEFINERT)
            .statsborgerskap(adresseLand);

        PersonAdresse.Builder adresseBuilder = PersonAdresse.builder().adresselinje1("Portveien 2").land(adresseLand);
        persona.adresse(adresseType, adresseBuilder);
        PersonInformasjon søker = persona.build();
        scenario.medRegisterOpplysninger(søker);
    }
}
