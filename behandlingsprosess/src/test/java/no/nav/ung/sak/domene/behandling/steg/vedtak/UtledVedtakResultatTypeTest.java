package no.nav.ung.sak.domene.behandling.steg.vedtak;

import no.nav.ung.kodeverk.behandling.BehandlingResultatType;
import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vedtak.VedtakResultatType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.ung.sak.test.util.Whitebox;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class UtledVedtakResultatTypeTest {

    private long counter = 100L;

    private BehandlingVedtakTjeneste vedtakTjeneste;
    private TestScenarioBuilder scenario;

    @BeforeEach
    public void setup() {
        scenario = TestScenarioBuilder.builderMedSøknad();
        vedtakTjeneste = new BehandlingVedtakTjeneste(null, scenario.mockBehandlingRepositoryProvider());
    }

    @Test
    public void vedtakResultatTypeSettesTilAVSLAG() {
        // Arrange
        scenario.medBehandlingsresultat(BehandlingResultatType.AVSLÅTT);
        Behandling behandling = scenario.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForInnvilget() {
        // Arrange
        scenario.medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        Behandling behandling = scenario.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForForeldrepengerEndret() {
        // Arrange
        scenario.medBehandlingsresultat(BehandlingResultatType.INNVILGET_ENDRING);
        Behandling behandling = scenario.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    private VedtakResultatType utled(Behandling behandling) {
        return vedtakTjeneste.utledVedtakResultatType(behandling);
    }

    /**
     * Behandling 1: Avslått
     * Behandling 2: Ingen endring
     */
    @Test
    public void vedtakResultatTypeSettesTilAVSLAGForIngenEndringNårForrigeBehandlingAvslått() {
        // Arrange
        scenario.medBehandlingsresultat(BehandlingResultatType.AVSLÅTT);
        Behandling behandling1 = scenario.lagMocked();
        Behandling behandling2 = lagRevurdering(behandling1, BehandlingResultatType.INGEN_ENDRING);

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling2);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    /**
     * Behandling 1: Innvilget
     * Behandling 2: Ingen endring
     * Behandling 3: Ingen endring
     */
    @Test
    public void vedtakResultatTypeSettesTilAVSLAGForIngenEndringNårBehandling1InnvilgetOgBehandling2IngenEndring() {
        // Arrange
        scenario.medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        Behandling behandling1 = scenario.lagMocked();
        Behandling behandling2 = lagRevurdering(behandling1, BehandlingResultatType.INGEN_ENDRING);
        Behandling behandling3 = lagRevurdering(behandling2, BehandlingResultatType.INGEN_ENDRING);

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling3);

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }


    private Behandling lagRevurdering(Behandling tidligereBehandling, BehandlingResultatType behandlingResultatType) {
        BehandlingÅrsak.Builder årsakBuilder = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_KONTROLL_REGISTER_INNTEKT);
        Behandling revurdering = Behandling.fraTidligereBehandling(tidligereBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(årsakBuilder)
            .medBehandlingResultatType(behandlingResultatType)
            .build();
        Whitebox.setInternalState(revurdering, "id", counter++);
        scenario.mockBehandlingRepository().lagre(revurdering, new BehandlingLås(tidligereBehandling.getId()));
        return revurdering;
    }
}
