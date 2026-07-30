package no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.prosesstask.api.ProsessTaskData;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.forbruktedager.FagsakperiodeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class BackfillPeriodeMaksDatoTaskTest {

    private static final LocalDate STARTDATO = LocalDate.of(2025, 1, 6); // mandag

    @Inject
    private EntityManager em;

    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private BehandlingRepository behandlingRepository;
    private BackfillPeriodeMaksDatoTask task;
    private Behandling behandling;

    @BeforeEach
    void setUp() {
        ungdomsprogramPeriodeRepository = new UngdomsprogramPeriodeRepository(em);
        behandlingRepository = new BehandlingRepository(em);
        task = new BackfillPeriodeMaksDatoTask(em);

        var fagsak = Fagsak.opprettNy(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAK123"), STARTDATO, STARTDATO.plusWeeks(52));
        em.persist(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
    }

    @Test
    void skal_opprette_maks_periode_for_grunnlag_uten_maks_periode() {
        // Arrange: lagre grunnlag uten maks_periode (bruker lagre-metoden uten harForlengetPeriode)
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(STARTDATO, STARTDATO.plusWeeks(52).minusDays(1))));

        // Verifiser at grunnlaget mangler maks_periode
        var grunnlagFør = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(grunnlagFør.getUngdomsprogramMaksPeriode()).isEmpty();

        // Act
        task.doTask(ProsessTaskData.forProsessTask(BackfillPeriodeMaksDatoTask.class));

        // Assert
        em.clear();
        var grunnlagEtter = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(grunnlagEtter.getUngdomsprogramMaksPeriode()).isPresent();

        var maksPeriode = grunnlagEtter.getUngdomsprogramMaksPeriode().get();
        assertThat(maksPeriode.harForlengetPeriode()).isFalse();
        assertThat(maksPeriode.getPeriodeMaksDato()).isPresent();
        assertThat(maksPeriode.getPeriodeMaksDato().get().getDayOfWeek())
            .isNotIn(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);
    }

    @Test
    void skal_sette_maksdato_til_fredag_når_beregning_havner_på_helg() {
        // Startdato mandag: 260 virkedager = 52 uker, tom = søndag → justeres til fredag
        var mandagStart = LocalDate.of(2025, 1, 6); // mandag
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(mandagStart, mandagStart.plusWeeks(52).minusDays(1))));

        task.doTask(ProsessTaskData.forProsessTask(BackfillPeriodeMaksDatoTask.class));

        em.clear();
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        var maksDato = grunnlag.getUngdomsprogramMaksPeriode().get().getPeriodeMaksDato().get();

        assertThat(maksDato.getDayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        // Fredag 2. januar 2026 (søndag 4. jan justert tilbake til fredag 2. jan)
        assertThat(maksDato).isEqualTo(LocalDate.of(2026, 1, 2));
    }

    @Test
    void skal_ikke_endre_grunnlag_som_allerede_har_maks_periode() {
        // Arrange: lagre grunnlag med maks_periode
        LocalDate eksisterendeMaksDato = STARTDATO.plusWeeks(52).minusDays(1);
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(STARTDATO, eksisterendeMaksDato)),
            false,
            eksisterendeMaksDato);

        // Act
        task.doTask(ProsessTaskData.forProsessTask(BackfillPeriodeMaksDatoTask.class));

        // Assert: maks_periode er uendret
        em.clear();
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(grunnlag.getUngdomsprogramMaksPeriode()).isPresent();
        assertThat(grunnlag.getUngdomsprogramMaksPeriode().get().getPeriodeMaksDato().get())
            .isEqualTo(eksisterendeMaksDato);
    }

    @Test
    void skal_være_idempotent_ved_gjentatt_kjøring() {
        // Arrange
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(),
            List.of(new UngdomsprogramPeriode(STARTDATO, STARTDATO.plusWeeks(52).minusDays(1))));

        // Act: kjør to ganger
        task.doTask(ProsessTaskData.forProsessTask(BackfillPeriodeMaksDatoTask.class));
        em.clear();
        task.doTask(ProsessTaskData.forProsessTask(BackfillPeriodeMaksDatoTask.class));

        // Assert: fortsatt bare én maks_periode-rad
        em.clear();
        var grunnlag = ungdomsprogramPeriodeRepository.hentGrunnlag(behandling.getId()).orElseThrow();
        assertThat(grunnlag.getUngdomsprogramMaksPeriode()).isPresent();
    }
}
