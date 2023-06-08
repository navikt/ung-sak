package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class DefaultSaksbehandlingsfristUtlederTest {

    private final SøknadRepository søknadRepository = mock();

    @Test
    void testUtledFrist() {
        var søknadsdato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
            .builderMedSøknad()
            .medSøknadDato(søknadsdato);
        Behandling behandling = testScenarioBuilder.lagMocked();
        SøknadEntitet søknadEntitet = testScenarioBuilder.medSøknad().build();

        when(søknadRepository.hentSøknad(behandling.getId())).thenReturn(søknadEntitet);

        LocalDateTime fristEnUke = lagFristUtleder("P1W").utledFrist(behandling);
        assertThat(fristEnUke).isEqualTo(søknadsdato.plusWeeks(1).atStartOfDay());

        LocalDateTime frist0Uker = lagFristUtleder("P0W").utledFrist(behandling);
        assertThat(frist0Uker).isEqualTo(søknadsdato.atStartOfDay());
    }

    private DefaultSaksbehandlingsfristUtleder lagFristUtleder(String periode) {
        return new DefaultSaksbehandlingsfristUtleder(søknadRepository, periode);
    }
}
