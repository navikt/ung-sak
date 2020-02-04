package no.nav.foreldrepenger.behandling.steg.vedtak;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import no.nav.foreldrepenger.behandling.steg.vedtak.UtledVedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsak;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.vedtak.VedtakResultatType;

public class UtledVedtakResultatTypeTest {

    private final LocalDate SKJÆRINGSTIDSPUNKT = LocalDate.now();

    private TestScenarioBuilder scenarioFørstegang;

    @Before
    public void setup() {
        scenarioFørstegang = TestScenarioBuilder.builderMedSøknad();
    }

    @Test
    public void vedtakResultatTypeSettesTilAVSLAG() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT));
        Behandling behandling = scenarioFørstegang.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling, Optional.empty(), Optional.empty());

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForInnvilget() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling = scenarioFørstegang.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling, Optional.empty(), Optional.empty());

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForForeldrepengerEndret() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET_ENDRING));
        Behandling behandling = scenarioFørstegang.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling, Optional.empty(), Optional.empty());

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    /**
     * Behandling 1: Avslått
     * Behandling 2: Ingen endring
     */
    @Test
    public void vedtakResultatTypeSettesTilAVSLAGForIngenEndringNårForrigeBehandlingAvslått() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.AVSLÅTT));
        Behandling behandling1 = scenarioFørstegang.lagMocked();
        Behandling behandling2 = lagRevurdering(behandling1, BehandlingResultatType.INGEN_ENDRING);

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling2, Optional.empty(), Optional.empty());

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
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling1 = scenarioFørstegang.lagMocked();
        Behandling behandling2 = lagRevurdering(behandling1, BehandlingResultatType.INGEN_ENDRING);
        Behandling behandling3 = lagRevurdering(behandling2, BehandlingResultatType.INGEN_ENDRING);

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling3, Optional.empty(), Optional.empty());

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void vedtakResultatTypeSettesTilINNVILGETForOpphørMedDatoEtterSkjæringstidspunkt() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.OPPHØR));
        Behandling behandling = scenarioFørstegang.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling, Optional.of(SKJÆRINGSTIDSPUNKT.plusDays(1)), Optional.of(SKJÆRINGSTIDSPUNKT));

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.INNVILGET);
    }

    @Test
    public void vedtakResultatTypeSettesTilAVSLAGForOpphørMedDatoLikSkjæringstidspunkt() {
        // Arrange
        scenarioFørstegang.medBehandlingsresultat(Behandlingsresultat.builder().medBehandlingResultatType(BehandlingResultatType.OPPHØR));
        Behandling behandling = scenarioFørstegang.lagMocked();

        // Act
        VedtakResultatType vedtakResultatType = UtledVedtakResultatType.utled(behandling, Optional.of(SKJÆRINGSTIDSPUNKT), Optional.of(SKJÆRINGSTIDSPUNKT));

        // Assert
        assertThat(vedtakResultatType).isEqualTo(VedtakResultatType.AVSLAG);
    }

    private Behandling lagRevurdering(Behandling tidligereBehandling, BehandlingResultatType behandlingResultatType) {
        BehandlingÅrsak.Builder årsakBuilder = BehandlingÅrsak.builder(BehandlingÅrsakType.RE_ENDRET_INNTEKTSMELDING)
            .medOriginalBehandling(tidligereBehandling);
        Behandling revurdering = Behandling.fraTidligereBehandling(tidligereBehandling, BehandlingType.REVURDERING)
            .medBehandlingÅrsak(årsakBuilder)
            .build();
        Behandlingsresultat.builder().medBehandlingResultatType(behandlingResultatType).buildFor(revurdering);
        return revurdering;
    }
}
