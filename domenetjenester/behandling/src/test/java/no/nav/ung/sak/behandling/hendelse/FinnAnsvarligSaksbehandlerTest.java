package no.nav.ung.sak.behandling.hendelse;

import static org.assertj.core.api.Assertions.assertThat;

import no.nav.ung.sak.behandlingslager.behandling.BehandlingAnsvarlig;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import org.junit.jupiter.api.Test;

import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;

public class FinnAnsvarligSaksbehandlerTest {

    private static final String BESLUTTER = "Beslutter";
    private static final String SAKSBEHANDLER = "Saksbehandler";

    private TestScenarioBuilder scenario = TestScenarioBuilder.builderMedSøknad();
    private Behandling behandling = scenario.lagMocked();
    private BehandlingAnsvarlig behandlingAnsvarlig = new BehandlingAnsvarlig(behandling.getId(), BehandlingDel.SENTRAL);

    @Test
    public void ansvarligSaksbehandlerSettesTilAnsvarligBeslutterNårSatt() {
        // Arrange
        behandlingAnsvarlig.setAnsvarligSaksbehandler(SAKSBEHANDLER);
        behandlingAnsvarlig.setAnsvarligBeslutter(BESLUTTER);

        // Act
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandlingAnsvarlig);

        // Assert
        assertThat(ansvarligSaksbehandler).isEqualTo(BESLUTTER);
    }

    @Test
    public void ansvarligSaksbehandlerSettesTilAnsvarligSaksbehandlerNårAnsvarligBeslutterIkkeErSatt() {
        // Arrange
        behandlingAnsvarlig.setAnsvarligSaksbehandler(SAKSBEHANDLER);

        // Act
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandlingAnsvarlig);

        // Assert
        assertThat(ansvarligSaksbehandler).isEqualTo(SAKSBEHANDLER);
    }

    @Test
    public void ansvarligSaksbehandlerSettesTilVLNårBeslutterOgSaksbehandlerMangler() {
        // Act
        String ansvarligSaksbehandler = FinnAnsvarligSaksbehandler.finn(behandlingAnsvarlig);

        // Assert
        assertThat(ansvarligSaksbehandler).isEqualTo("VL");

    }
}
