package no.nav.k9.sak.domene.registerinnhenting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.domene.medlem.MedlemTjeneste;
import no.nav.k9.sak.domene.person.tps.PersoninfoAdapter;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.test.util.behandling.AbstractTestScenario;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.cdi.UnitTestInstanceImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RegisterdataInnhenterTest {

    private String durationInstance = "PT10H";

    @Test
    public void skal_innhente_registeropplysninger_på_nytt_når_det_ble_hentet_inn_i_går() {
        // Arrange
        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusDays(1));
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isTrue();
    }

    @Test
    public void skal_ikke_innhente_registeropplysninger_på_nytt_når_det_nettopp_har_blitt_hentet_inn() {
        // Arrange
        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now());
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    @Test
    public void skal_ikke_innhente_registeropplysninger_på_nytt_når_det_ikke_har_blitt_hentet_inn_tidligere() {
        // Arrange
        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(null);
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    @Test
    public void skal_innhente_registeropplysninger_ut_ifra_midnatt_når_konfigurasjonsverdien_mangler() {
        // Arrange
        LocalDateTime midnatt = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(midnatt.minusMinutes(1));
        Behandling behandling = scenario.lagMocked();
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, null);

        // Act
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isTrue();
    }

    @Test
    public void skal_innhente_registeropplysninger_mellom_midnatt_og_klokken_3_men_ikke_ellers_grunnet_konfigverdien() {
        // Arrange
        LocalDateTime midnatt = LocalDate.now().atStartOfDay();
        LocalDateTime opplysningerOppdatertTidspunkt = midnatt.minusHours(1); // en time før midnatt

        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(opplysningerOppdatertTidspunkt);

        RegisterdataEndringshåndterer registerdataOppdatererEngangsstønad = lagRegisterdataInnhenter(scenario, "PT3H");

        // Act
        Boolean skalInnhente = registerdataOppdatererEngangsstønad.erOpplysningerOppdatertTidspunktFør(midnatt,
            Optional.of(opplysningerOppdatertTidspunkt));

        // Assert
        assertThat(skalInnhente).isTrue();
    }

    @Test
    public void skal_ikke_innhente_opplysninger_på_nytt_selvom_det_ble_hentet_inn_i_går_fordi_konfigverdien_er_mer_enn_midnatt() {
        // Arrange
        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusHours(20));
        Behandling behandling = scenario.lagMocked();

        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, "PT30H");

        // Act
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    private RegisterdataEndringshåndterer lagRegisterdataInnhenter(AbstractTestScenario<?> scenario, String durationInstance) {
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        return lagRegisterdataOppdaterer(repositoryProvider, durationInstance);
    }

    private RegisterdataEndringshåndterer lagRegisterdataOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                                    String durationInstance) {

        RegisterdataInnhenter innhenter = lagRegisterdataInnhenter(repositoryProvider);

        RegisterdataEndringshåndterer oppdaterer = lagRegisterdataOppdaterer(repositoryProvider, durationInstance, innhenter);
        return oppdaterer;
    }

    private RegisterdataEndringshåndterer lagRegisterdataOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                                    String durationInstance, RegisterdataInnhenter innhenter) {

        RegisterdataEndringshåndterer oppdaterer = new RegisterdataEndringshåndterer(repositoryProvider, innhenter, durationInstance, null, null, null, null, null);
        return oppdaterer;
    }

    private RegisterdataInnhenter lagRegisterdataInnhenter(BehandlingRepositoryProvider repositoryProvider) {
        var skjæringstidspunktTjeneste = mock(SkjæringstidspunktTjeneste.class);

        PersoninfoAdapter personinfoAdapter = mock(PersoninfoAdapter.class);
        MedlemTjeneste medlemTjeneste = mock(MedlemTjeneste.class);

        return new RegisterdataInnhenter(personinfoAdapter,
            medlemTjeneste,
            repositoryProvider,
            null,
            skjæringstidspunktTjeneste, null,
            new UnitTestInstanceImpl<>(null));
    }
}
