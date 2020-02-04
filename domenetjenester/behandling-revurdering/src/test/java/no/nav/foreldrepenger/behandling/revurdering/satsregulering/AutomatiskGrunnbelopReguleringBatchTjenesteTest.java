package no.nav.foreldrepenger.behandling.revurdering.satsregulering;


import static no.nav.foreldrepenger.behandling.revurdering.satsregulering.AutomatiskGrunnbelopReguleringTask.TASKTYPE;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningSatsType;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AutomatiskGrunnbelopReguleringBatchTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @Inject
    private ProsessTaskRepository prosessTaskRepository;

    @Inject
    private BeregningsresultatRepository beregningsresultatRepository;

    @Inject
    private BehandlingRevurderingRepository behandlingRevurderingRepository;

    private AutomatiskGrunnbelopReguleringBatchTask tjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;

    private long gammelSats;
    private long nySats;
    private LocalDate cutoff;

    @Before
    public void setUp() {
        nySats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getVerdi();
        cutoff = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, LocalDate.now()).getPeriode().getFomDato();
        gammelSats = beregningsresultatRepository.finnEksaktSats(BeregningSatsType.GRUNNBELØP, cutoff.minusDays(1)).getVerdi();
        tjeneste = new AutomatiskGrunnbelopReguleringBatchTask(behandlingRevurderingRepository, beregningsresultatRepository, prosessTaskRepository);
    }

    @Test
    public void skal_finne_en_sak_å_revurdere() {
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        tjeneste.doTask(new ProsessTaskData(AutomatiskGrunnbelopReguleringBatchTask.TASKTYPE));
        assertThat(prosessTaskRepository.finnIkkeStartet().stream().anyMatch(task -> task.getTaskType().equals(TASKTYPE))).isTrue();
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering() {
        opprettRevurderingsKandidat(BehandlingStatus.UTREDES, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        tjeneste.doTask(new ProsessTaskData(AutomatiskGrunnbelopReguleringBatchTask.TASKTYPE));
        assertThat(prosessTaskRepository.finnIkkeStartet().stream().filter(it -> it.getTaskType().equals(AutomatiskGrunnbelopReguleringTask.TASKTYPE)).collect(Collectors.toList())).hasSize(0);
    }

    @Test
    public void skal_finne_to_saker_å_revurdere() {
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.minusDays(5));  // FØR
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 6 * gammelSats, cutoff.plusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, gammelSats, 4 * gammelSats, cutoff.plusDays(5)); // Ikke avkortet
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, nySats, 6 * nySats, cutoff.plusDays(5)); // Ny sats
        final var prosessTaskData = new ProsessTaskData(AutomatiskGrunnbelopReguleringBatchTask.TASKTYPE);
        prosessTaskData.setProperty(AutomatiskGrunnbelopReguleringBatchTask.KEY_DRY_RUN, "false");
        tjeneste.doTask(prosessTaskData);

        assertThat(prosessTaskRepository.finnIkkeStartet().stream().filter(it -> it.getTaskType().equals(AutomatiskGrunnbelopReguleringTask.TASKTYPE)).collect(Collectors.toList())).hasSize(2);
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status, long sats, long avkortet, LocalDate uttakFom) {
        LocalDate dato = uttakFom.plusWeeks(3);

        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medSøknadDato(dato.minusDays(40));

        scenario.medBehandlingsresultat(Behandlingsresultat.builderForInngangsvilkår().medBehandlingResultatType(BehandlingResultatType.INNVILGET));
        Behandling behandling = scenario.lagre(repositoryProvider);

        Whitebox.setInternalState(behandling, "status", status);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(dato.minusWeeks(3L))
            .medGrunnbeløp(BigDecimal.valueOf(sats))
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSTAKER)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
            .medAvkortetPrÅr(BigDecimal.valueOf(avkortet))
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode.builder(periode)
            .build(beregningsgrunnlag);
        beregningsgrunnlagTjeneste.lagreBeregningsgrunnlag(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.FASTSATT);

        BeregningsresultatEntitet brFP = BeregningsresultatEntitet.builder()
            .medRegelInput("clob1")
            .medRegelSporing("clob2")
            .build();
        BeregningsresultatPeriode.builder()
            .medBeregningsresultatPeriodeFomOgTom(uttakFom, uttakFom.plusMonths(3))
            .medBeregningsresultatAndeler(Collections.emptyList())
            .build(brFP);
        repositoryProvider.getBeregningsresultatRepository().lagre(behandling, brFP);
        repoRule.getRepository().flushAndClear();
        return repoRule.getEntityManager().find(Behandling.class, behandling.getId());
    }

}
