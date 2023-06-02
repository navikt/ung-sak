package no.nav.k9.sak.behandling.revurdering.etterkontroll.saksbehandlingstid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadEntitet;
import no.nav.k9.sak.behandlingslager.behandling.søknad.SøknadRepository;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class DefaultSaksbehandlingsfristUtlederTest {

    private final SøknadRepository søknadRepository = mock();
    private final DefaultSaksbehandlingsfristUtleder utleder =
        new DefaultSaksbehandlingsfristUtleder(søknadRepository, 1L);

    @Test
    void testUtledFrist() {
        LocalDate søknadsdato = LocalDateTime.now().toLocalDate();
        TestScenarioBuilder testScenarioBuilder = TestScenarioBuilder
            .builderMedSøknad()
            .medSøknadDato(søknadsdato);
        Behandling behandling = testScenarioBuilder.lagMocked();
        SøknadEntitet søknadEntitet = testScenarioBuilder.medSøknad().build();

        when(søknadRepository.hentSøknad(behandling.getId())).thenReturn(søknadEntitet);

        LocalDateTime frist = utleder.utledFrist(behandling);

        assertThat(frist.toLocalDate()).isEqualTo(søknadsdato.plusWeeks(1));
    }
}
