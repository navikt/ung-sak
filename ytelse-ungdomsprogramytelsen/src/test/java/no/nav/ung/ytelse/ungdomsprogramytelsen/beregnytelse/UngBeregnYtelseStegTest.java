package no.nav.ung.ytelse.ungdomsprogramytelsen.beregnytelse;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.k9.felles.testutilities.cdi.CdiAwareExtension;
import no.nav.k9.felles.testutilities.cdi.UnitTestLookupInstanceImpl;
import no.nav.ung.kodeverk.kontroll.KontrollertInntektKilde;
import no.nav.ung.kodeverk.ungdomsytelse.sats.UngdomsytelseSatsType;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriode;
import no.nav.ung.sak.behandlingslager.perioder.UngdomsprogramPeriodeRepository;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.KontrollertInntektPeriode;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseRepository;
import no.nav.ung.sak.behandlingslager.ytelse.UngdomsytelseGrunnlagRepository;
import no.nav.ung.sak.behandlingslager.ytelse.sats.UngdomsytelseSatser;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPeriode;
import no.nav.ung.sak.behandlingslager.ytelse.uttak.UngdomsytelseUttakPerioder;
import no.nav.ung.sak.db.util.JpaExtension;
import no.nav.ung.sak.domene.abakus.AbakusInMemoryInntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.behandling.steg.beregnytelse.BeregnYtelseSteg;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.OppgittOpptjeningBuilder;
import no.nav.ung.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.TestScenarioBuilder;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestRepositories;
import no.nav.ung.sak.test.util.behandling.ungdomsprogramytelse.UngTestScenario;
import no.nav.ung.ytelse.ungdomsprogramytelsen.perioder.UngdomsytelseKvalifiserteYtelsesperioderTjeneste;
import no.nav.ung.ytelse.ungdomsprogramytelsen.perioder.UngdomsytelseInntektskontrollFritakUtleder;
import no.nav.ung.ytelse.ungdomsprogramytelsen.ungdomsprogrammet.UngdomsprogramPeriodeTjeneste;
import no.nav.ung.sak.ytelseperioder.MånedsvisTidslinjeUtleder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(JpaExtension.class)
@ExtendWith(CdiAwareExtension.class)
class UngBeregnYtelseStegTest {

    public static final LocalDate FOM = LocalDate.of(2026, 11, 12);

    @Inject
    private EntityManager entityManager;
    private BeregnYtelseSteg beregnYtelseSteg;
    private UngdomsytelseGrunnlagRepository ungdomsytelseGrunnlagRepository;
    private TilkjentYtelseRepository tilkjentYtelseRepository;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private UngdomsprogramPeriodeRepository ungdomsprogramPeriodeRepository;
    private BehandlingRepository behandlingRepository;
    private Behandling behandling;
    private UngTestRepositories ungTestRepositories;

    @BeforeEach
    void setUp() {
        inntektArbeidYtelseTjeneste = new AbakusInMemoryInntektArbeidYtelseTjeneste();
        ungTestRepositories = UngTestRepositories.lagAlleUngTestRepositoriesOgAbakusTjeneste(entityManager, inntektArbeidYtelseTjeneste);
        ungdomsytelseGrunnlagRepository = ungTestRepositories.ungdomsytelseGrunnlagRepository();
        tilkjentYtelseRepository = ungTestRepositories.tilkjentYtelseRepository();
        ungdomsprogramPeriodeRepository = ungTestRepositories.ungdomsprogramPeriodeRepository();
        behandlingRepository = ungTestRepositories.repositoryProvider().getBehandlingRepository();
        final var månedsvisTidslinjeUtleder = new MånedsvisTidslinjeUtleder(
            new UnitTestLookupInstanceImpl<>(
                new UngdomsytelseKvalifiserteYtelsesperioderTjeneste(new UngdomsprogramPeriodeTjeneste(ungdomsprogramPeriodeRepository), ungdomsytelseGrunnlagRepository)),
            behandlingRepository);
        beregnYtelseSteg = new UngBeregnYtelseSteg(ungdomsytelseGrunnlagRepository,
            tilkjentYtelseRepository,
            månedsvisTidslinjeUtleder,
            new UngdomsytelseInntektskontrollFritakUtleder()
        );
    }

    @Test
    void skal_opprette_tilkjent_ytelse_med_sporing_og_input() {
        lagScenario(null);
        final var behandleStegResultat = beregnYtelseSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));
        final var tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId());
        assertThat(tilkjentYtelse.get().getInput()).isNotNull();
        assertThat(tilkjentYtelse.get().getSporing()).isNotNull();
    }

    @Test
    void skal_få_tilkjent_ytelse_i_periode_uten_inntekt_med_gjenomført_kontroll() {
        final var kontrollertInntektPeriode = KontrollertInntektPeriode.ny()
            .medPeriode(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusMonths(1).withDayOfMonth(1), FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())))
            .medInntekt(BigDecimal.ZERO)
            .medKilde(KontrollertInntektKilde.BRUKER)
            .build();
        lagScenario(new LocalDateTimeline<>(kontrollertInntektPeriode.getPeriode().toLocalDateInterval(), kontrollertInntektPeriode));
        final var behandleStegResultat = beregnYtelseSteg.utførSteg(new BehandlingskontrollKontekst(behandling.getFagsakId(), behandling.getAktørId(), behandlingRepository.taSkriveLås(behandling.getId())));
        final var tilkjentYtelse = tilkjentYtelseRepository.hentTilkjentYtelse(behandling.getId());
        assertThat(tilkjentYtelse.get().getInput()).isNotNull();
        assertThat(tilkjentYtelse.get().getSporing()).isNotNull();

        final var perioder = tilkjentYtelse.get().getPerioder();
        assertThat(perioder.size()).isEqualTo(2);
        assertThat(perioder.get(0).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM, FOM.with(TemporalAdjusters.lastDayOfMonth())));
        assertThat(perioder.get(1).getPeriode()).isEqualTo(DatoIntervallEntitet.fraOgMedTilOgMed(FOM.plusMonths(1).withDayOfMonth(1), FOM.plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())));
    }

    private void lagScenario(LocalDateTimeline<KontrollertInntektPeriode> kontrollerteInntektPerioder) {
        final var satser = new UngdomsytelseSatser(BigDecimal.valueOf(1000),
            BigDecimal.TEN,
            BigDecimal.ONE,
            UngdomsytelseSatsType.LAV, 0, 0);
        final var uttakperioder = new UngdomsytelseUttakPerioder(List.of(new UngdomsytelseUttakPeriode(
            DatoIntervallEntitet.fraOgMedTilOgMed(FOM, FOM.plusWeeks(52))
        )));
        uttakperioder.setRegelInput("test");
        uttakperioder.setRegelSporing("test");
        final var scenario = new UngTestScenario(
            "Beregning",
            List.of(new UngdomsprogramPeriode(DatoIntervallEntitet.fraOgMed(FOM))),
            new LocalDateTimeline<>(FOM, FOM.plusWeeks(52), satser),
            uttakperioder,
            null,
            null,
            null,
            FOM.minusYears(19),
            List.of(FOM),
            Set.of(),
            List.of(),
            null,
            kontrollerteInntektPerioder,
            null,
            false
        );
        behandling = TestScenarioBuilder.builderMedSøknad()
            .medSøknadDato(FOM)
            .medUngTestGrunnlag(scenario)
            .buildOgLagreMedUng(ungTestRepositories);
        inntektArbeidYtelseTjeneste.lagreOppgittOpptjening(behandling.getId(), OppgittOpptjeningBuilder.ny());
    }

}
