package no.nav.ung.ytelse.ungdomsprogramytelsen.vurderkompletthet.ungdomsprogramkontroll;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.impl.ProsessTaskTjenesteImpl;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.varsel.EtterlysningStatus;
import no.nav.ung.kodeverk.varsel.EtterlysningType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.etterlysning.Etterlysning;
import no.nav.ung.sak.behandlingslager.etterlysning.EtterlysningRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.trigger.Trigger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class AutomatiskOpphørEtterlysningTjenesteTest {

    private AutomatiskOpphørEtterlysningTjeneste tjeneste;

    @Inject
    private ProsessTaskTjenesteImpl prosessTaskTjeneste;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    private EtterlysningRepository etterlysningRepository;
    @Inject
    private EntityManager entityManager;

    private ProsessTriggereRepository prosessTriggereRepository;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        prosessTriggereRepository = new ProsessTriggereRepository(entityManager);
        var scenario = TestScenarioBuilder.builderMedSøknad();
        behandling = scenario.lagre(entityManager);
        tjeneste = new AutomatiskOpphørEtterlysningTjeneste(
            etterlysningRepository,
            ungdomsprogramPeriodeRepository,
            prosessTaskTjeneste,
            prosessTriggereRepository
        );
    }

    @Test
    void skal_opprette_etterlysning_for_automatisk_opphor() {
        var fom = LocalDate.now();
        var tom = fom.plusMonths(6);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, tom)));

        // Sett opp trigger med maksdato
        prosessTriggereRepository.leggTil(behandling.getId(), Set.of(
            new Trigger(BehandlingÅrsakType.RE_VARSEL_AUTOMATISK_OPPHOR, DatoIntervallEntitet.fraOgMedTilOgMed(tom, tom))
        ));

        tjeneste.opprettEtterlysningForAutomatiskOpphør(BehandlingReferanse.fra(behandling));

        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).hasSize(1);
        var etterlysning = etterlysninger.get(0);
        assertThat(etterlysning.getType()).isEqualTo(EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR);
        assertThat(etterlysning.getStatus()).isEqualTo(EtterlysningStatus.OPPRETTET);
        assertThat(etterlysning.getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom));
    }

    @Test
    void skal_ikke_opprette_ny_etterlysning_dersom_ventende_finnes() {
        var fom = LocalDate.now();
        var tom = fom.plusMonths(6);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, tom)));
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        // Opprett en eksisterende etterlysning på vent
        var eksisterende = Etterlysning.opprettForType(
            behandling.getId(), grunnlag.getGrunnlagsreferanse(), UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR);
        eksisterende.vent(LocalDateTime.now());
        etterlysningRepository.lagre(eksisterende);

        // Act
        tjeneste.opprettEtterlysningForAutomatiskOpphør(BehandlingReferanse.fra(behandling));

        // Assert - skal fortsatt bare være 1
        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).hasSize(1);
        assertThat(etterlysninger.get(0).getStatus()).isEqualTo(EtterlysningStatus.VENTER);
    }

    @Test
    void skal_avbryte_eksisterende_etterlysning() {
        var fom = LocalDate.now();
        var tom = fom.plusMonths(6);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(fom, tom)));
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();

        // Opprett en eksisterende etterlysning på vent
        var eksisterende = Etterlysning.opprettForType(
            behandling.getId(), grunnlag.getGrunnlagsreferanse(), UUID.randomUUID(),
            DatoIntervallEntitet.fraOgMedTilOgMed(fom, tom), EtterlysningType.UTTALELSE_AUTOMATISK_OPPHOR);
        eksisterende.vent(LocalDateTime.now());
        etterlysningRepository.lagre(eksisterende);

        // Act
        tjeneste.avbrytEtterlysningForAutomatiskOpphør(BehandlingReferanse.fra(behandling));

        // Assert
        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).hasSize(1);
        assertThat(etterlysninger.get(0).getStatus()).isEqualTo(EtterlysningStatus.SKAL_AVBRYTES);
    }

    @Test
    void skal_ikke_feile_ved_avbryt_uten_eksisterende_etterlysning() {
        // Act - skal ikke kaste exception
        tjeneste.avbrytEtterlysningForAutomatiskOpphør(BehandlingReferanse.fra(behandling));

        // Assert
        var etterlysninger = etterlysningRepository.hentEtterlysninger(behandling.getId());
        assertThat(etterlysninger).isEmpty();
    }
}

