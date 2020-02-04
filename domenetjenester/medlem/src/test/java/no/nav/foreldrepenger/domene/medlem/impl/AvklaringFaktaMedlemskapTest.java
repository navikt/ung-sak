package no.nav.foreldrepenger.domene.medlem.impl;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.AbstractTestScenario;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektBuilder;
import no.nav.foreldrepenger.domene.iay.modell.InntektspostBuilder;
import no.nav.foreldrepenger.domene.iay.modell.VersjonType;
import no.nav.foreldrepenger.domene.medlem.MedlemskapPerioderTjeneste;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.iay.InntektsKilde;
import no.nav.k9.kodeverk.iay.InntektspostType;
import no.nav.k9.kodeverk.medlemskap.MedlemskapDekningType;
import no.nav.k9.kodeverk.medlemskap.MedlemskapType;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AvklaringFaktaMedlemskapTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private MedlemskapPerioderTjeneste medlemskapPerioderTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    private AvklaringFaktaMedlemskap tjeneste;

    private static final LocalDate SKJÆRINGSDATO_FØDSEL = LocalDate.now().plusDays(1);

    @Before
    public void setUp() {
        this.tjeneste = new AvklaringFaktaMedlemskap(provider, medlemskapPerioderTjeneste, personopplysningTjeneste, iayTjeneste);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_ved_gyldig_medlems_periode() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);

        MedlemskapPerioderEntitet gyldigPeriode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_7_a) // hjemlet i bokstav a
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();
        scenario.leggTilMedlemskapPeriode(gyldigPeriode);

        leggTilSøker(scenario);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_ved_dekningsgrad_lik_ikke_medlem() {
        // Arrange
        MedlemskapPerioderEntitet gyldigPeriode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.FTL_2_9_1_b)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilMedlemskapPeriode(gyldigPeriode);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_få_aksjonspunkt_når_dekningsrad_er_av_type_uavklart() {
        // Arrange
        MedlemskapPerioderEntitet gyldigPeriode = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.OPPHOR)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilMedlemskapPeriode(gyldigPeriode);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);

        leggTilSøker(scenario);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).contains(MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_dekningsgrad_unntatt_og_person_bosatt_og_statsborgerskap_ulik_usa() {
        // Arrange
        MedlemskapPerioderEntitet medlemskapPeriodeForUnntak = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.UNNTATT) // unntak FT §2-13
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilMedlemskapPeriode(medlemskapPeriodeForUnntak);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_dersom_dekningsgrad_unntatt_og_person_utvandret() {
        // Arrange
        MedlemskapPerioderEntitet medlemskapPeriodeForUnntak = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.UNNTATT) // unntak FT §2-13
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilMedlemskapPeriode(medlemskapPeriodeForUnntak);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.USA, Region.UDEFINERT, PersonstatusType.UTVA);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_opprette_aksjonspunkt_dersom_dekningsgrad_unntatt_og_person_bosatt_og_statsborgerskap_lik_usa() {
        // Arrange
        MedlemskapPerioderEntitet medlemskapPeriodeForUnntak = new MedlemskapPerioderBuilder()
            .medDekningType(MedlemskapDekningType.UNNTATT)
            .medMedlemskapType(MedlemskapType.ENDELIG)
            .medErMedlem(true)
            .medPeriode(SKJÆRINGSDATO_FØDSEL, SKJÆRINGSDATO_FØDSEL)
            .build();

        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.leggTilMedlemskapPeriode(medlemskapPeriodeForUnntak);
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.USA, Region.UDEFINERT, PersonstatusType.BOSA);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).contains(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_status_utvandret() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.NOR, Region.NORDEN, PersonstatusType.UTVA);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_ikke_utvandret_og_region_nordisk() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.SWE, Region.UDEFINERT, PersonstatusType.BOSA);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_ikke_utvandret_og_region_eøs_og_inntekt_siste_3mnd() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        var aktørId = leggTilSøker(scenario, Landkoder.BEL, Region.EOS, PersonstatusType.BOSA);
        Behandling behandling = lagre(scenario);

        // Arrange - inntekt
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørInntekt = builder.getAktørInntektBuilder(aktørId);
        aktørInntekt.leggTilInntekt(InntektBuilder.oppdatere(empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING)
            .leggTilInntektspost(InntektspostBuilder.ny()
                .medBeløp(BigDecimal.TEN)
                .medInntektspostType(InntektspostType.LØNN)
                .medPeriode(SKJÆRINGSDATO_FØDSEL.minusMonths(2), SKJÆRINGSDATO_FØDSEL.minusMonths(1))));
        builder.leggTilAktørInntekt(aktørInntekt);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).isEmpty();
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(provider);
    }

    @Test
    public void skal_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_ikke_utvandret_og_region_eøs_og_ikke_inntekt_siste_3mnd() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.BEL, Region.UDEFINERT, PersonstatusType.BOSA);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    @Test
    public void skal_opprette_aksjonspunkt_ved_ikke_gyldig_periode_og_ikke_utvandret_og_region_annen() {
        // Arrange
        var scenario = TestScenarioBuilder.builderMedSøknad();
        scenario.medSøknad().medMottattDato(SKJÆRINGSDATO_FØDSEL);
        leggTilSøker(scenario, Landkoder.UDEFINERT, Region.UDEFINERT, PersonstatusType.BOSA);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> resultat = tjeneste.utled(behandling, SKJÆRINGSDATO_FØDSEL);

        // Assert
        assertThat(resultat).contains(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD);
    }

    private void leggTilSøker(AbstractTestScenario<?> scenario) {
        leggTilSøker(scenario, Landkoder.NOR, Region.NORDEN, PersonstatusType.BOSA);
    }

    private AktørId leggTilSøker(AbstractTestScenario<?> scenario, Landkoder statsborgerskap, Region region, PersonstatusType personstatus) {
        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();
        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.UOPPGITT, region)
            .personstatus(personstatus)
            .statsborgerskap(statsborgerskap)
            .build();
        scenario.medRegisterOpplysninger(søker);
        return søkerAktørId;
    }

}
