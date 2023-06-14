package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class PsbSaksbehandlingsfristUtlederTest {

    private final SøknadRepository søknadRepository = mock();

    @Test
    void skal_utlede_frist() {
        var søknadsdato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
            .builderMedSøknad()
            .medBehandlingType(BehandlingType.FØRSTEGANGSSØKNAD)
            .medSøknadDato(søknadsdato);
        Behandling behandling = testScenarioBuilder.lagMocked();
        SøknadEntitet søknadEntitet = testScenarioBuilder.medSøknad().build();

        when(søknadRepository.hentSøknadHvisEksisterer(behandling.getId())).thenReturn(Optional.of(søknadEntitet));

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).get().isEqualTo(søknadsdato.plusWeeks(1).atStartOfDay());

        var frist0Uker = lagFristUtleder("P0W").utledFrist(behandling);
        assertThat(frist0Uker).get().isEqualTo(søknadsdato.atStartOfDay());
    }

    @Test
    void skal_ikke_utlede_frist_for_revurderinger() {
        var søknadsdato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
            .builderMedSøknad()
            .medBehandlingType(BehandlingType.REVURDERING)
            .medSøknadDato(søknadsdato);
        Behandling behandling = testScenarioBuilder.lagMocked();

        var fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).isEmpty();

    }

    private PsbSaksbehandlingsfristUtleder lagFristUtleder(String periode) {
        return new PsbSaksbehandlingsfristUtleder(søknadRepository, periode);
    }
}
