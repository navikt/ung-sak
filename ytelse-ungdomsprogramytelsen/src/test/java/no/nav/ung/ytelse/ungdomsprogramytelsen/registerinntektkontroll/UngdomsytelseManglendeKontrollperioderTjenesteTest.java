package no.nav.ung.ytelse.ungdomsprogramytelsen.registerinntektkontroll;

import jakarta.inject.Inject;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.behandlingslager.fagsak.FagsakRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.kontroll.ManglendeKontrollperioderTjeneste;
import no.nav.ung.sak.kontroll.RelevanteKontrollperioderUtleder;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;
import no.nav.ung.sak.ytelseperioder.KvalifiserteYtelsesperioderTjeneste;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(CdiAwareExtension.class)
@ExtendWith(JpaExtension.class)
class UngdomsytelseManglendeKontrollperioderTjenesteTest {

    @Inject
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    @Inject
    private BehandlingRepository behandlingRepository;
    @Inject
    private FagsakRepository fagsakRepository;
    @Inject
    private RelevanteKontrollperioderUtleder relevanteKontrollperioderUtleder;
    private Behandling behandling;
    private MånedsvisTidslinjeUtleder ytelsesperiodeutleder;
    private KvalifiserteYtelsesperioderTjeneste kvalifiserteYtelsesperioderTjeneste;


    @BeforeEach
    void setUp() {

        kvalifiserteYtelsesperioderTjeneste = Mockito.mock(KvalifiserteYtelsesperioderTjeneste.class);
        when(kvalifiserteYtelsesperioderTjeneste.finnPeriodeTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        when(kvalifiserteYtelsesperioderTjeneste.finnInitiellPeriodeTidslinje(anyLong())).thenReturn(LocalDateTimeline.empty());
        ytelsesperiodeutleder = new MånedsvisTidslinjeUtleder(new UnitTestLookupInstanceImpl<>(kvalifiserteYtelsesperioderTjeneste), behandlingRepository);
        lagFagsakOgBehandling(LocalDate.now().minusMonths(6));
    }

    @Test
    void skal_ikke_legge_ulede_manglende_kontroll_dersom_kun_en_måned() {

        var manglendeKontrollperioderTjeneste = lagTjeneste(6);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        lagPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram);

        final var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling);

        assertThat(perioder.isEmpty()).isTrue();
    }

    @Test
    void skal_ikke_ulede_manglende_kontroll_dersom_programdeltakelse_over_en_måned() {

        var manglendeKontrollperioderTjeneste = lagTjeneste(6);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(3).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(3).with(TemporalAdjusters.lastDayOfMonth());

        lagPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram);

        final var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling);

        assertThat(perioder.isEmpty()).isTrue();
    }


    @Test
    void skal_ulede_manglende_kontroll_for_måned_nr_to_dersom_programdeltakelse_over_tre_måneder_og_passert_rapporteringsfrist() {

        var manglendeKontrollperioderTjeneste = lagTjeneste(6);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(4).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());

        lagPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram);

        final var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling);

        assertThat(perioder.isEmpty()).isFalse();
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3).withDayOfMonth(1), LocalDate.now().minusMonths(3).with(TemporalAdjusters.lastDayOfMonth()));
        assertThat(perioder.iterator().next()).isEqualTo(månedNrTre);
    }

    @Test
    void skal_ikke_ulede_manglende_kontroll_dersom_ikke_passert_kontrolldato() {
        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        var manglendeKontrollperioderTjeneste = lagTjeneste(LocalDate.now().getDayOfMonth() + 1);
        lagPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram);

        var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling);

        assertThat(perioder.isEmpty()).isTrue();
    }

    public boolean erFørsteDagIMåneden() {
        return LocalDate.now().getDayOfMonth() == 1;
    }

    @Test
    @DisabledIf("erFørsteDagIMåneden")
    void skal_ulede_manglende_kontroll_dersom_passert_kontrolldato_med_en_dag() {
        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        var manglendeKontrollperioderTjeneste = lagTjeneste(LocalDate.now().getDayOfMonth() - 1);

        lagPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram);

        final var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling);

        assertThat(perioder.size()).isEqualTo(1);
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1).withDayOfMonth(1), LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
        assertThat(perioder.iterator().next()).isEqualTo(månedNrTre);
    }


    @Test
    void skal_ulede_manglende_kontroll_dersom_dagens_dato_er_lik_kontrolldato() {
        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(2).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().with(TemporalAdjusters.lastDayOfMonth());

        var manglendeKontrollperioderTjeneste = lagTjeneste(LocalDate.now().getDayOfMonth());
        lagPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram);

        final var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling);

        assertThat(perioder.size()).isEqualTo(1);
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(1).withDayOfMonth(1), LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth()));
        assertThat(perioder.iterator().next()).isEqualTo(månedNrTre);
    }

    @Test
    void skal_ikke_ulede_manglende_kontroll_dersom_allerede_kontrollert() {

        var manglendeKontrollperioderTjeneste = lagTjeneste(6);

        final var startdatoUngdomsprogram = LocalDate.now().minusMonths(4).withDayOfMonth(1);
        final var sluttdatoUngdomsprogram = LocalDate.now().minusMonths(2).with(TemporalAdjusters.lastDayOfMonth());
        final var månedNrTre = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusMonths(3).withDayOfMonth(1), LocalDate.now().minusMonths(3).with(TemporalAdjusters.lastDayOfMonth()));
        final var sisteMåned = DatoIntervallEntitet.fraOgMedTilOgMed(sluttdatoUngdomsprogram.withDayOfMonth(1), sluttdatoUngdomsprogram);

        lagPeriode(startdatoUngdomsprogram, sluttdatoUngdomsprogram);
        tilkjentYtelseRepository.lagre(behandling.getId(), List.of(
            KontrollertInntektPeriode.ny().medPeriode(månedNrTre).medInntekt(BigDecimal.ZERO).medKilde(KontrollertInntektKilde.BRUKER).medErManueltVurdert(false).build(),
            KontrollertInntektPeriode.ny().medPeriode(sisteMåned).medInntekt(BigDecimal.ZERO).medKilde(KontrollertInntektKilde.BRUKER).medErManueltVurdert(false).build()
        ));

        var perioder = manglendeKontrollperioderTjeneste.finnPerioderForManglendeKontroll(behandling);

        assertThat(perioder.isEmpty()).isTrue();
    }

    private void lagPeriode(LocalDate startdatoUngdomsprogram, LocalDate sluttdatoUngdomsprogram) {
        when(kvalifiserteYtelsesperioderTjeneste.finnPeriodeTidslinje(anyLong())).thenReturn(new LocalDateTimeline<>(startdatoUngdomsprogram, sluttdatoUngdomsprogram, true));
    }

    private Long lagFagsakOgBehandling(LocalDate fom) {
        final var fagsak = new Fagsak(FagsakYtelseType.UNGDOMSYTELSE, AktørId.dummy(), new Saksnummer("SAKEN"), fom, fom.plusWeeks(52));
        fagsakRepository.opprettNy(fagsak);
        behandling = Behandling.forFørstegangssøknad(fagsak).build();
        behandlingRepository.lagre(behandling, behandlingRepository.taSkriveLås(behandling));
        return behandling.getId();
    }

    private ManglendeKontrollperioderTjeneste lagTjeneste(int dagIMånedForInntektsKontroll) {
        ZonedDateTime nå = ZonedDateTime.now();
        ZonedDateTime tiSekunderSiden = nå.minusSeconds(10);
        // Dersom denne kjøre innenfor 10 sekunder etter midnatt og dagIMånedForInntektsKontroll er lik dagens dato, vil denne generere feil cron-uttrykk
        String tidspunktCron = nå.toLocalDate().getDayOfMonth() == dagIMånedForInntektsKontroll ? tiSekunderSiden.getSecond() + " " + (tiSekunderSiden.getMinute()) + " " + tiSekunderSiden.getHour() : "0 0 " + nå.getHour();
        String cron = tidspunktCron + " " + dagIMånedForInntektsKontroll + " * *";
        return new ManglendeKontrollperioderTjeneste(ytelsesperiodeutleder, cron, tilkjentYtelseRepository, relevanteKontrollperioderUtleder);
    }
}
