package no.nav.k9.sak.domene.person.personopplysning;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.db.util.JpaExtension;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.k9.sak.test.util.behandling.personopplysning.PersonInformasjon;
import no.nav.k9.sak.typer.AktørId;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UtlandVurdererTjenesteTest {

    @Inject
    private EntityManager entityManager;

    private BehandlingRepositoryProvider repositoryProvider;
    private TestScenarioBuilder scenario;

    private PersonopplysningTjeneste personopplysningTjeneste;

    @BeforeEach
    public void before() {

        repositoryProvider = new BehandlingRepositoryProvider(entityManager);
        scenario = TestScenarioBuilder.builderMedSøknad();

        personopplysningTjeneste = new PersonopplysningTjeneste(repositoryProvider.getPersonopplysningRepository());
    }

    @Test
    void skal_ikke_svare_som_utland_der_pleietrengende_har_fnr_og_ikke_markert_som_utenlandsk() {
        var behandling = lagBehandlingMedPleietrengendestatus(PersonstatusType.BOSA);

        assertThat(lag().erUtenlandssak(behandling)).isFalse();

    }

    @Test
    void skal_svare_som_utland_der_pleietrengende_har_dnr() {
        var behandling = lagBehandlingMedPleietrengendestatus(PersonstatusType.ADNR);

        assertThat(lag().erUtenlandssak(behandling)).isTrue();

    }

    private Behandling lagBehandlingMedPleietrengendestatus(PersonstatusType bosa) {
        var mor = scenario.getDefaultBrukerAktørId();
        var pleietrengende = AktørId.dummy();

        scenario.medSøknad();

        PersonInformasjon.Builder builder = scenario
            .opprettBuilderForRegisteropplysninger();

        PersonInformasjon barn = builder
            .medPersonas()
            .barn(pleietrengende, LocalDate.now().minusYears(4), bosa)
            .relasjonTil(mor, RelasjonsRolleType.MORA, null)
            .build();


        PersonInformasjon søker = builder
            .medPersonas()
            .kvinne(mor, SivilstandType.GIFT, Region.NORDEN)
            .statsborgerskap(Landkoder.NOR)
            .personstatus(PersonstatusType.BOSA)
            .relasjonTil(pleietrengende, RelasjonsRolleType.BARN, null)
            .build();

        scenario.medRegisterOpplysninger(barn);
        scenario.medRegisterOpplysninger(søker);


        return scenario.medPleietrengende(pleietrengende).lagre(repositoryProvider);
    }


    @Test
    void skal_svare_som_utland_for_automatiske_utenlandssaker() {
        var mottattDato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder builder = lagScenario(BehandlingType.FØRSTEGANGSSØKNAD, mottattDato);

        builder.leggTilAksjonspunkt(
            AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK,
            AksjonspunktDefinisjon.AUTOMATISK_MARKERING_AV_UTENLANDSSAK.getBehandlingSteg());

        Behandling behandling = builder.lagMocked();

        assertThat(lag().erUtenlandssak(behandling)).isTrue();

    }


    @Test
    void skal_svare_som_utland_for_manuelle_utenlandssaker() {
        var mottattDato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder builder = lagScenario(BehandlingType.FØRSTEGANGSSØKNAD, mottattDato);

        builder.leggTilAksjonspunkt(
            AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE,
            AksjonspunktDefinisjon.MANUELL_MARKERING_AV_UTLAND_SAKSTYPE.getBehandlingSteg());

        Behandling behandling = builder.lagMocked();

        assertThat(lag().erUtenlandssak(behandling)).isTrue();

    }

    private UtlandVurdererTjeneste lag() {
        return new UtlandVurdererTjeneste(personopplysningTjeneste);
    }


    private static TestScenarioBuilder lagScenario(BehandlingType førstegangssøknad, LocalDate mottattDato) {
        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
            .builderMedSøknad()
            .medBehandlingType(førstegangssøknad);
        testScenarioBuilder.medSøknad().medMottattDato(mottattDato);
        return testScenarioBuilder;
    }

}
