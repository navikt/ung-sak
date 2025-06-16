package no.nav.ung.sak.perioder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateSegment;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class ProsessTriggerPeriodeUtlederTest {

    @Inject
    private BehandlingRepositoryProvider behandlingRepositoryProvider;
    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private Long behandlingId;


    @BeforeEach
    void setUp() {
        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandlingId = scenario.lagre(behandlingRepositoryProvider).getId();
        prosessTriggerPeriodeUtleder = new ProsessTriggerPeriodeUtleder(prosessTriggereRepository, null);
    }

    @Test
    void skal_ikke_finne_perioder_for_ingen_triggere() {
        final var resultat = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);
        assertThat(resultat.isEmpty()).isTrue();
    }

    @Test
    void skal_finne_en_periode_trigger() {
        // Arrange
        final var fom = LocalDate.now();
        final var tom = LocalDate.now().plusDays(10);
        prosessTriggereRepository.leggTil(behandlingId, Set.of(new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))));
        // Act
        final var resultat = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);
        // Assert
        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(fom, tom, Set.of(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)));
    }

    @Test
    void skal_finne_en_periode_for_to_overlappende_triggere() {
        // Arrange
        final var fom = LocalDate.now();
        final var tom = LocalDate.now().plusDays(10);
        prosessTriggereRepository.leggTil(behandlingId, Set.of(
            new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)),
            new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom))
        ));
        // Act
        final var resultat = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);
        // Assert
        assertThat(resultat).isEqualTo(new LocalDateTimeline<>(fom, tom, Set.of(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS)));
    }

    @Test
    void skal_finne_tidslinje_for_to_ikke_overlappende_triggere() {
        // Arrange
        final var fom = LocalDate.now();
        final var tom = LocalDate.now().plusDays(10);
        final var fom2 = LocalDate.now().plusDays(12);
        final var tom2 = LocalDate.now().plusDays(22);
        prosessTriggereRepository.leggTil(behandlingId, Set.of(
            new Trigger(BehandlingÅrsakType.RE_HENDELSE_FØDSEL, DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom)),
            new Trigger(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS, DatoIntervallEntitet.fraOgMedTilOgMed(fom2, tom2))
        ));
        // Act
        final var resultat = prosessTriggerPeriodeUtleder.utledTidslinje(behandlingId);
        // Assert
        final var forventetResultat = new LocalDateTimeline<>(List.of(new LocalDateSegment<>(fom, tom, Set.of(BehandlingÅrsakType.RE_HENDELSE_FØDSEL)), new LocalDateSegment<>(fom2, tom2, Set.of(BehandlingÅrsakType.RE_TRIGGER_BEREGNING_HØY_SATS))));
        assertThat(resultat).isEqualTo(forventetResultat);
    }

}
