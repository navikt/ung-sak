package no.nav.k9.sak.domene.medlem.impl;

import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import no.nav.k9.kodeverk.arbeidsforhold.InntektsKilde;
import no.nav.k9.kodeverk.arbeidsforhold.InntektspostType;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.UnittestRepositoryRule;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseAggregatBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektBuilder;
import no.nav.k9.sak.domene.iay.modell.InntektspostBuilder;
import no.nav.k9.sak.domene.iay.modell.VersjonType;
import no.nav.k9.sak.domene.person.personopplysning.PersonopplysningTjeneste;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.test.util.behandling.personopplysning.PersonAdresse;
import no.nav.k9.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.k9.sak.test.util.behandling.personopplysning.Personas;
import no.nav.k9.sak.typer.AktørId;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AvklarOmSøkerOppholderSegINorgeTest {

    @Rule
    public UnittestRepositoryRule repoRule = new UnittestRepositoryRule();
    private BehandlingRepositoryProvider provider = new BehandlingRepositoryProvider(repoRule.getEntityManager());

    @Inject
    private PersonopplysningTjeneste personopplysningTjeneste;

    @Inject
    private InntektArbeidYtelseTjeneste iayTjeneste;

    private AvklarOmSøkerOppholderSegINorge tjeneste;

    @BeforeEach
    public void setUp() {
        this.tjeneste = new AvklarOmSøkerOppholderSegINorge(personopplysningTjeneste, iayTjeneste);
    }

    private Behandling lagre(AbstractTestScenario<?> scenario) {
        return scenario.lagre(provider);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_er_nordisk() {
        // Arrange
        LocalDate termindato = LocalDate.now();
        AktørId aktørId1 = AktørId.dummy();

        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.FIN);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    private Optional<MedlemResultat> kallTjeneste(Behandling behandling, LocalDate dato) {
        var ref = BehandlingReferanse.fra(behandling, dato);
        return tjeneste.utled(ref, dato);
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_har_annet_statsborgerskap() {
        // Arrange
        LocalDate termindato = LocalDate.now();
        AktørId aktørId1 = AktørId.dummy();

        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.CAN);
        Behandling behandling = lagre(scenario);

        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_er_gift_med_nordisk() {
        // Arrange
        LocalDate termindato = LocalDate.now();

        var scenario = TestScenarioBuilder.builderMedSøknad();

        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        AktørId annenPartAktørId = AktørId.dummy();

        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon gift = builderForRegisteropplysninger
            .medPersonas()
            .mann(annenPartAktørId, SivilstandType.GIFT, Region.NORDEN)
            .statsborgerskap(Landkoder.FIN)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.EKTE)
            .build();
        scenario.medRegisterOpplysninger(gift);

        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT, Region.EOS)
            .statsborgerskap(Landkoder.ESP)
            .relasjonTil(annenPartAktørId, RelasjonsRolleType.EKTE)
            .build();

        scenario.medRegisterOpplysninger(søker);

        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_er_gift_med_ANNET_statsborgerskap() {
        // Arrange
        LocalDate termindato = LocalDate.now();

        var scenario = TestScenarioBuilder.builderMedSøknad();

        AktørId søkerAktørId = scenario.getDefaultBrukerAktørId();
        AktørId annenPartAktørId = AktørId.dummy();

        PersonInformasjon.Builder builderForRegisteropplysninger = scenario.opprettBuilderForRegisteropplysninger();

        PersonInformasjon søker = builderForRegisteropplysninger
            .medPersonas()
            .kvinne(søkerAktørId, SivilstandType.GIFT, Region.EOS)
            .statsborgerskap(Landkoder.ESP)
            .relasjonTil(annenPartAktørId, RelasjonsRolleType.EKTE)
            .build();

        PersonInformasjon gift = builderForRegisteropplysninger
            .medPersonas()
            .mann(annenPartAktørId, SivilstandType.GIFT, Region.UDEFINERT)
            .statsborgerskap(Landkoder.CAN)
            .relasjonTil(søkerAktørId, RelasjonsRolleType.EKTE)
            .build();

        scenario.medRegisterOpplysninger(gift);
        scenario.medRegisterOpplysninger(søker);
        Behandling behandling = lagre(scenario);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    @Test
    public void skal_ikke_opprette_aksjonspunkt_om_soker_har_hatt_inntekt_i_Norge_de_siste_tre_mnd() {
        // Arrange
        AktørId aktørId1 = AktørId.dummy();
        LocalDate fom = LocalDate.now().minusWeeks(3L);
        LocalDate tom = LocalDate.now().minusWeeks(1L);

        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        LocalDate termindato = LocalDate.now().plusDays(40);
        scenario.medSøknad().medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);
        Behandling behandling = lagre(scenario);

        leggTilInntekt(behandling, behandling.getAktørId(), fom, tom);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).isEmpty();
    }

    private void leggTilInntekt(Behandling behandling, AktørId aktørId, LocalDate fom, LocalDate tom) {
        // Arrange - inntekt
        var builder = InntektArbeidYtelseAggregatBuilder.oppdatere(empty(), VersjonType.REGISTER);
        var aktørInntekt = builder.getAktørInntektBuilder(aktørId);
        aktørInntekt.leggTilInntekt(InntektBuilder.oppdatere(empty())
            .medInntektsKilde(InntektsKilde.INNTEKT_OPPTJENING)
            .leggTilInntektspost(InntektspostBuilder.ny()
                .medBeløp(BigDecimal.TEN)
                .medInntektspostType(InntektspostType.LØNN)
                .medPeriode(fom, tom)));
        builder.leggTilAktørInntekt(aktørInntekt);
        iayTjeneste.lagreIayAggregat(behandling.getId(), builder);
    }

    @Test
    public void skal_ikke_opprette_vent_om_termindato_har_passert_28_dager() {
        // Arrange
        LocalDate termindato = LocalDate.now().minusMonths(2);
        AktørId aktørId1 = AktørId.dummy();
        LocalDate fom = LocalDate.now().minusWeeks(60L);
        LocalDate tom = LocalDate.now().minusWeeks(58L);

        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        scenario.medSøknad().medMottattDato(termindato.minusMonths(2).plusDays(3));
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);

        Behandling behandling = lagre(scenario);

        leggTilInntekt(behandling, behandling.getAktørId(), fom, tom);

        // Act
        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    @Test
    public void skal_opprette_aksjonspunkt_ved_uavklart_oppholdsrett() {
        // Arrange
        LocalDate termindato = LocalDate.now().minusDays(15L);
        AktørId aktørId1 = AktørId.dummy();

        var scenario = TestScenarioBuilder.builderMedSøknad().medBruker(aktørId1);
        scenario.medSøknad()
            .medMottattDato(LocalDate.now());
        leggTilSøker(scenario, AdresseType.POSTADRESSE_UTLAND, Landkoder.ESP);
        Behandling behandling = lagre(scenario);

        Optional<MedlemResultat> medlemResultat = kallTjeneste(behandling, termindato);

        //Assert
        assertThat(medlemResultat).contains(MedlemResultat.AVKLAR_OPPHOLDSRETT);
    }

    private AktørId leggTilSøker(AbstractTestScenario<?> scenario, AdresseType adresseType, Landkoder adresseLand) {
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
        return søkerAktørId;
    }

}
