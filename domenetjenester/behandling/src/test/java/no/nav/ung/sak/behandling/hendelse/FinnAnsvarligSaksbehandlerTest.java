package no.nav.ung.sak.behandling.hendelse;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;

public class FinnAnsvarligSaksbehandlerTest {

    private static final String BESLUTTER = "Beslutter";
    private static final String SAKSBEHANDLER = "Saksbehandler";

    private Behandling behandling;

    @BeforeEach
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
