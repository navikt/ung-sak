package no.nav.ung.sak.domene.registerinnhenting;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.AbstractTestScenario;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class RegisterdataEndringshåndtererTest {

    private String durationInstance = "PT10H";

    @Test
    public void skal_innhente_registeropplysninger_på_nytt_når_det_ble_hentet_inn_i_går() {
        // Arrange
        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusDays(1));
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance, null);
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isTrue();
    }


    @Test
    public void skal_innhente_registeropplysninger_på_nytt_hvis_eldre_enn_overstyrt_duration() {
        // Arrange
        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusHours(1));
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance, "PT30M");
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isTrue();
    }

    @Test
    public void skal_ikke_innhente_registeropplysninger_på_nytt_hvis_nyere_enn_overstyrt_duration() {
        // Arrange
        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now().minusHours(1));
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance, "PT2H");
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }


    @Test
    public void skal_ikke_innhente_registeropplysninger_på_nytt_når_det_nettopp_har_blitt_hentet_inn() {
        // Arrange
        var scenario = TestScenarioBuilder
            .builderMedSøknad()
            .medOpplysningerOppdatertTidspunkt(LocalDateTime.now());
        Behandling behandling = scenario.lagMocked();

        // Act
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance, null);
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
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, durationInstance, null);
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
        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, null, null);

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

        RegisterdataEndringshåndterer registerdataOppdatererEngangsstønad = lagRegisterdataInnhenter(scenario, "PT3H", null);

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

        RegisterdataEndringshåndterer registerdataEndringshåndterer = lagRegisterdataInnhenter(scenario, "PT30H", null);

        // Act
        Boolean harHentetInn = registerdataEndringshåndterer.skalInnhenteRegisteropplysningerPåNytt(behandling);

        // Assert
        assertThat(harHentetInn).isFalse();
    }

    private RegisterdataEndringshåndterer lagRegisterdataInnhenter(AbstractTestScenario<?> scenario, String durationInstance, String overstyrtInnhentingDuration) {
        BehandlingRepositoryProvider repositoryProvider = scenario.mockBehandlingRepositoryProvider();
        return lagRegisterdataOppdaterer(repositoryProvider, durationInstance, overstyrtInnhentingDuration);
    }

    private RegisterdataEndringshåndterer lagRegisterdataOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                                                    String durationInstance, String overstyrtInnhentingDuration) {

        RegisterdataEndringshåndterer oppdaterer = new RegisterdataEndringshåndterer(repositoryProvider, durationInstance, null, null, null, null, overstyrtInnhentingDuration);
        return oppdaterer;
    }

}
