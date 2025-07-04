package no.nav.ung.sak.domene.behandling.steg.registerinntektkontroll;

import jakarta.inject.Inject;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseStartdatoRepository;
import no.nav.ung.sak.behandlingslager.behandling.startdato.UngdomsytelseSøktStartdato;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.perioder.ProsessTriggerPeriodeUtleder;
import no.nav.ung.sak.perioder.UngdomsytelseSøknadsperiodeTjeneste;
import no.nav.ung.sak.trigger.ProsessTriggereRepository;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ungdomsprogram.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelse.kontroll.ManglendeKontrollperioderTjeneste;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class ManglendeKontrollperioderTjenesteTest {

    @Inject
    private ProsessTriggereRepository prosessTriggereRepository;
    @Inject
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    @Inject
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private UngdomsytelseStartdatoRepository ungdomsytelseStartdatoRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    private Behandling behandling;
    private ProsessTriggerPeriodeUtleder prosessTriggerPeriodeUtleder;
    private MånedsvisTidslinjeUtleder ytelsesperiodeutleder;


    @BeforeEach
    void setUp() {
        final var ungdomsprogramPeriodeTjeneste = new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository, ungdomsytelseStartdatoRepository);
        final var ungdomsytelseSøknadsperiodeTjeneste = new UngdomsytelseSøknadsperiodeTjeneste(ungdomsytelseStartdatoRepository, ungdomsprogramPeriodeTjeneste, behandlingRepository);
        prosessTriggerPeriodeUtleder = new ProsessTriggerPeriodeUtleder(prosessTriggereRepository, ungdomsytelseSøknadsperiodeTjeneste);
        ytelsesperiodeutleder = new MånedsvisTidslinjeUtleder(ungdomsprogramPeriodeTjeneste, behandlingRepository);
        lagFagsakOgBehandling(LocalDate.now().minusMonths(6));
    }

    @Test
    void skal_ikke_legge_ulede_manglende_kontroll_dersom_kun_en_måned() {

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(
            ytelsesperiodeutleder,
            prosessTriggerPeriodeUtleder, 6, tilkjentYtelseRepository);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        final var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling.getId());

        assertThat(perioder.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_ulede_manglende_kontroll_dersom_programdeltakelse_over_to_måneder() {

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(ytelsesperiodeutleder, prosessTriggerPeriodeUtleder, 6, tilkjentYtelseRepository);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(3).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        final var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling.getId());

        assertThat(perioder.isEmpty()).isTrue();
    }

    @Test
    void skal_ulede_manglende_kontroll_for_måned_nr_to_dersom_programdeltakelse_over_tre_måneder_og_passert_rapporteringsfrist() {

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(ytelsesperiodeutleder, prosessTriggerPeriodeUtleder, 6, tilkjentYtelseRepository);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(4).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        final var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling.getId());

        assertThat(perioder.isEmpty()).isFalse();
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3).withDayOfMonth(1), LocalDate.now().minusMonths(3).with(TemporalAdjusters.lastDayOfMonth()));
        assertThat(perioder.iterator().next()).isEqualTo(månedNrTre);
    }

    @Test
    void skal_ikke_ulede_manglende_kontroll_dersom_ikke_passert_rapporteringsfrist() {
        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(ytelsesperiodeutleder, prosessTriggerPeriodeUtleder, LocalDate.now().getDayOfMonth(), tilkjentYtelseRepository);


        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling.getId());

        assertThat(perioder.isEmpty()).isTrue();
    }


    @Test
    void skal_ulede_manglende_kontroll_dersom_passert_rapporteringsfrist_med_en_dag() {
        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(ytelsesperiodeutleder, prosessTriggerPeriodeUtleder, LocalDate.now().getDayOfMonth() - 1, tilkjentYtelseRepository);


        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));

        final var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling.getId());

        assertThat(perioder.size()).isEqualTo(1);
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1).withDayOfMonth(1), LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
        assertThat(perioder.iterator().next()).isEqualTo(månedNrTre);
    }

    @Test
    void skal_ikke_ulede_manglende_kontroll_dersom_allerede_kontrollert() {

        var manglendeKontrollperioderTjeneste = new ManglendeKontrollperioderTjeneste(ytelsesperiodeutleder, prosessTriggerPeriodeUtleder, 6, tilkjentYtelseRepository);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(4).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3).withDayOfMonth(1), LocalDate.now().minusMonths(3).with(TemporalAdjusters.lastDayOfMonth()));

        ungdomsytelseStartdatoRepository.lagre(behandling.getId(), List.of(new UngdomsytelseSøktStartdato(startdatoUngdomsprogram, new JournalpostId(1L))));
        ungdomsprogramPeriodeRepository.lagre(behandling.getId(), List.of(new UngdomsprogramPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram)));
        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(KontrollertInntektPeriode.ny().medPeriode(månedNrTre).medInntekt(BigDecimal.ZERO).medKilde(KontrollertInntektKilde.BRUKER).medErManueltVurdert(false).build()));

        var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling.getId());

        assertThat(perioder.isEmpty()).isTrue();
    }


    private Long lagFagsakOgBehandling(LocalDate fom) {
        final var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, fom.plusWeeks(52));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling.getId();
    }
}
