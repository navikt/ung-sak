package no.nav.foreldrepenger.behandling.revurdering.satsregulering;


import java.time.LocalDate;
import java.util.Collections;

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
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.TestScenarioBuilder;
import no.nav.foreldrepenger.dbstoette.UnittestRepositoryRule;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingStatus;
import no.nav.k9.kodeverk.beregningsgrunnlag.BeregningsgrunnlagTilstand;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;
import no.nav.vedtak.felles.testutilities.Whitebox;
import no.nav.vedtak.felles.testutilities.cdi.CdiRunner;

@RunWith(CdiRunner.class)
public class AutomatiskArenaReguleringBatchTjenesteTest {

    @Rule
    public final UnittestRepositoryRule repoRule = new UnittestRepositoryRule();

    @Inject
    private BehandlingRepository behandlingRepository;

    @Inject
    private BeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;

    @Inject
    private ProsessTaskRepository prosessTaskRepositoryMock;

    private AutomatiskArenaReguleringBatchTask tjeneste;

    @Inject
    private BehandlingRepositoryProvider repositoryProvider;
    
    private LocalDate cutoff;
    private LocalDate nySatsDato;
    private ProsessTaskData taskData;


    @Before
    public void setUp() throws Exception {
        cutoff = AutomatiskArenaReguleringBatchTask.DATO;
        nySatsDato = cutoff.plusWeeks(3);
        tjeneste = new AutomatiskArenaReguleringBatchTask(repositoryProvider, prosessTaskRepositoryMock);
        taskData = new ProsessTaskData(AutomatiskArenaReguleringBatchTask.TASKTYPE);
        taskData.setProperty(AutomatiskArenaReguleringBatchTask.KEY_DRY_RUN, "false");
        taskData.setProperty(AutomatiskArenaReguleringBatchTask.KEY_SATS_DATO, nySatsDato.format(AutomatiskArenaReguleringBatchTask.DATE_FORMATTER));
    }

    @Test
    public void skal_ikke_finne_saker_til_revurdering() {
        opprettRevurderingsKandidat(BehandlingStatus.UTREDES, cutoff.minusDays(5));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.plusMonths(2));
        opprettRevurderingsKandidat(BehandlingStatus.AVSLUTTET, cutoff.minusDays(5));
        tjeneste.doTask(taskData);
    }

    private Behandling opprettRevurderingsKandidat(BehandlingStatus status, LocalDate uttakFom) {
        LocalDate dato = uttakFom.plusWeeks(3);

        var scenario = TestScenarioBuilder.builderMedSøknad()
            .medSøknadDato(dato.minusDays(40));

        scenario.medBehandlingsresultat(BehandlingResultatType.INNVILGET);
        Behandling behandling = scenario.lagre(repositoryProvider);

        Whitebox.setInternalState(behandling, "status", status);

        BehandlingLås lås = behandlingRepository.taSkriveLås(behandling);
        behandlingRepository.lagre(behandling, lås);

        BeregningsgrunnlagEntitet beregningsgrunnlag = BeregningsgrunnlagEntitet.builder()
            .medSkjæringstidspunkt(uttakFom)
            .build();
        BeregningsgrunnlagAktivitetStatus.builder()
            .medAktivitetStatus(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)
            .build(beregningsgrunnlag);
        BeregningsgrunnlagPeriode periode = BeregningsgrunnlagPeriode.builder()
            .medBeregningsgrunnlagPeriode(uttakFom, uttakFom.plusMonths(3))
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
