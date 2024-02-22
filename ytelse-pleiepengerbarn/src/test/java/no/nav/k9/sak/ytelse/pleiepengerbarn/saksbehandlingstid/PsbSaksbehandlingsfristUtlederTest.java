package no.nav.k9.sak.ytelse.pleiepengerbarn.saksbehandlingstid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.domene.person.personopplysning.UtlandVurdererTjeneste;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class PsbSaksbehandlingsfristUtlederTest {

    private final SøknadRepository søknadRepository = mock();
    private final UtlandVurdererTjeneste utlandVurdererTjeneste = mock();

    @BeforeEach
    void setup(){
        when(utlandVurdererTjeneste.erUtenlandssak(any())).thenReturn(false);
    }

    @Test
    void skal_utlede_frist() {
        var mottattDato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder testScenarioBuilder = lagScenario(BehandlingType.FØRSTEGANGSSØKNAD, mottattDato);
        Behandling behandling = testScenarioBuilder.lagMocked();
        SøknadEntitet søknadEntitet = testScenarioBuilder.medSøknad().build();

        when(søknadRepository.hentSøknadHvisEksisterer(behandling.getId())).thenReturn(Optional.of(søknadEntitet));

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).get().isEqualTo(mottattDato.plusWeeks(1).atStartOfDay());

        var frist0Uker = lagFristUtleder("P0W").utledFrist(behandling);
        assertThat(frist0Uker).get().isEqualTo(mottattDato.atStartOfDay());
    }

    @Test
    void skal_ikke_utlede_frist_for_revurderinger() {
        var mottattDato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder testScenarioBuilder = lagScenario(BehandlingType.REVURDERING, mottattDato);
        Behandling behandling = testScenarioBuilder.lagMocked();

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).isEmpty();

    }

    @Test
    void skal_ikke_utlede_frist_for_avsluttet_behandling() {
        var mottattDato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder testScenarioBuilder = lagScenario(BehandlingType.FØRSTEGANGSSØKNAD, mottattDato);
        Behandling behandling = testScenarioBuilder.lagMocked();
        behandling.avsluttBehandling();

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).isEmpty();

    }

    @Test
    void skal_ikke_utlede_frist_for_utenlandssaker() {
        var mottattDato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder builder = lagScenario(BehandlingType.FØRSTEGANGSSØKNAD, mottattDato);

        when(utlandVurdererTjeneste.erUtenlandssak(any())).thenReturn(true);

        Behandling behandling = builder.lagMocked();

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).isEmpty();

    }

    private static TestScenarioBuilder lagScenario(BehandlingType førstegangssøknad, LocalDate mottattDato) {
        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
            .builderMedSøknad()
            .medBehandlingType(førstegangssøknad);
        testScenarioBuilder.medSøknad().medMottattDato(mottattDato);
        return testScenarioBuilder;
    }


    private PsbSaksbehandlingsfristUtleder lagFristUtleder(String periode) {
        return new PsbSaksbehandlingsfristUtleder(søknadRepository, Period.parse(periode), utlandVurdererTjeneste);
    }
}