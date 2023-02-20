package no.nav.k9.sak.domene.medlem.impl;

import static java.util.Collections.singletonList;
import static no.nav.k9.sak.domene.medlem.impl.MedlemResultat.AVKLAR_OM_ER_BOSATT;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.medlem.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlem.MedlemskapType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapOppgittLandOppholdEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.k9.sak.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.test.util.behandling.personopplysning.PersonAdresse;
import no.nav.k9.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.k9.sak.test.util.behandling.personopplysning.Personas;
import no.nav.k9.sak.typer.AktørId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
public class AvklarOmErBosattTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider provider;

    @Inject
    private MedlemskapRepository medlemskapRepository;

    @Inject
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    private AvklarOmErBosatt avklarOmErBosatt;

    @BeforeEach
    public void setUp() {
        provider = new BehandlingRepositoryProvider(entityManager);
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
    public void skal_opprette_aksjonspunkt_dersom_bruker_oppgir_utenlandsopphold() {
        // Arrange
        LocalDate fødselsDato = LocalDate.now();
        MedlemskapOppgittLandOppholdEntitet oppholdUtlandForrigePeriode = new MedlemskapOppgittLandOppholdEntitet.Builder()
            .medLand(Landkoder.BEL)
            .medPeriode(LocalDate.now(), LocalDate.now().plusDays(1)) // periodens start/lengde påvirker ikke utledning
            .build();
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medDefaultOppgittTilknytning();
        leggTilSøker(scenario, AdresseType.BOSTEDSADRESSE, Landkoder.NOR);
        scenario.medOppgittTilknytning().medOpphold(singletonList(oppholdUtlandForrigePeriode));
        Behandling behandling = scenario.lagre(provider);

        // Act
        Optional<MedlemResultat> resultat = avklarOmErBosatt.utled(behandling, fødselsDato);

        // Assert
        assertThat(resultat).contains(AVKLAR_OM_ER_BOSATT);
    }


    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_ikke_oppgitt_opphold_utland() {
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
            .personstatus(PersonstatusType.BOSA)
            .statsborgerskap(adresseLand);

        PersonAdresse.Builder adresseBuilder = PersonAdresse.builder().adresselinje1("Portveien 2").land(adresseLand);
        persona.adresse(adresseType, adresseBuilder);
        PersonInformasjon søker = persona.build();
        scenario.medRegisterOpplysninger(søker);
    }
}
