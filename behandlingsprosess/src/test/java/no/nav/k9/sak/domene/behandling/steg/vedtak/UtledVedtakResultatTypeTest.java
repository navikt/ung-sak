package no.nav.k9.sak.domene.behandling.steg.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.k9.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.vedtak.felles.testutilities.Whitebox;

public class UtledVedtakResultatTypeTest {

    private long counter = 100L;

    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private BehandlingVedtakTjeneste vedtakTjeneste;
    private TestScenarioBuilder scenario;

    @BeforeEach
    public void setup() {
        scenario = TestScenarioBuilder.builderMedSøknad();
        vedtakTjeneste = new BehandlingVedtakTjeneste(null, scenario.mockBehandlingRepositoryProvider(), null);
    }

    @Test
    public void vedtakResultatTypeSettesTilAVSLAG() {
        // Arrange
        scenario.medBehandlingsresultat(BehandlingResultatType.AVSLÅTT);
        Behandling behandling = scenario.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling, Optional.empty(), Optional.empty());

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForInnvilget() {
        // Arrange
        scenario.medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        Behandling behandling = scenario.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling, Optional.empty(), Optional.empty());

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForForeldrepengerEndret() {
        // Arrange
        scenario.medBehandlingsresultat(BehandlingResultatType.INNVILGET_ENDRING);
        Behandling behandling = scenario.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling, Optional.empty(), Optional.empty());

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    private VedtakResultatType utled(Behandling behandling, Optional<LocalDate> opphørsdato, Optional<LocalDate> skjæringstidspunkt) {
        return vedtakTjeneste.utledVedtakResultatType(behandling, opphørsdato, skjæringstidspunkt);
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
        VedtakResultatType vedtakResultatType = utled(behandling2, Optional.empty(), Optional.empty());

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
        VedtakResultatType vedtakResultatType = utled(behandling3, Optional.empty(), Optional.empty());

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForOpphørMedDatoEtterSkjæringstidspunkt() {
        // Arrange
        Behandling behandling = scenario.medBehandlingsresultat(BehandlingResultatType.OPPHØR).lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling, Optional.of(SKJÆRINGSTIDSPUNKT.plusDays(1)), Optional.of(SKJÆRINGSTIDSPUNKT));

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void vedtakResultatTypeSettesTilAVSLAGForOpphørMedDatoLikSkjæringstidspunkt() {
        // Arrange
        Behandling behandling = scenario.medBehandlingsresultat(BehandlingResultatType.OPPHØR).lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = utled(behandling, Optional.of(SKJÆRINGSTIDSPUNKT), Optional.of(SKJÆRINGSTIDSPUNKT));

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    private Behandling lagRevurdering(Behandling tidligereBehandling, BehandlingResultatType behandlingResultatType) {
        BehandlingÅrsak.Builder årsakBuilder = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING);
        Behandling revurdering = Behandling.fraTidligereBehandling(tidligereBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(årsakBuilder)
            .medBehandlingResultatType(behandlingResultatType)
            .build();
        Whitebox.setInternalState(revurdering, "id", counter++);
        scenario.mockBehandlingRepository().lagre(revurdering, new BehandlingLås(tidligereBehandling.getId()));
        return revurdering;
    }
}
