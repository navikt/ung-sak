package no.nav.foreldrepenger.behandling.hendelse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.hendelse.FinnAnsvarligSaksbehandler;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;

public class FinnAnsvarligSaksbehandlerTest {

    private static final String BESLUTTER = "Beslutter";
    private static final String SAKSBEHANDLER = "Saksbehandler";

    private Behandling behandling;

    @Before
    public void setup() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagMocked();
    }

    @Test
    public void ansvarligSaksbehandlerSettesTilAnsvarligBeslutterNårSatt() {
        // Arrange
        behandling.setAnsvarligSaksbehandler(SAKSBEHANDLER);
        behandling.setAnsvarligBeslutter(BESLUTTER);

        // Act
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);

        // Assert
        assertThat(ansvarligSaksbehandler).isEqualTo(BESLUTTER);
    }

    @Test
    public void ansvarligSaksbehandlerSettesTilAnsvarligSaksbehandlerNårAnsvarligBeslutterIkkeErSatt() {
        // Arrange
        behandling.setAnsvarligSaksbehandler(SAKSBEHANDLER);

        // Act
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);

        // Assert
        assertThat(ansvarligSaksbehandler).isEqualTo(SAKSBEHANDLER);
    }

    @Test
    public void ansvarligSaksbehandlerSettesTilVLNårBeslutterOgSaksbehandlerMangler() {
        // Act
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandling);

        // Assert
        assertThat(ansvarligSaksbehandler).isEqualTo("VL");

    }
}
