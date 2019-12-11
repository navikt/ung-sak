package no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling;

import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagBuilder;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

public class BeregningAktivitetScenario implements TestScenarioTillegg {
    private BeregningAktivitetAggregatEntitet.Builder beregningAktiviteterBuilder;

    BeregningAktivitetScenario() {
        this.beregningAktiviteterBuilder = BeregningAktivitetAggregatEntitet.builder();
    }

    BeregningAktivitetAggregatEntitet.Builder getBeregningAktiviteterBuilder() {
        return beregningAktiviteterBuilder;
    }

    @Override
    public void lagre(Behandling behandling, RepositoryProvider repositoryProvider) {
        BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
        BeregningAktivitetAggregatEntitet beregningAktiviteter = beregningAktiviteterBuilder.build();
        BeregningsgrunnlagGrunnlagBuilder builder = BeregningsgrunnlagGrunnlagBuilder.oppdatere(Optional.empty()).medRegisterAktiviteter(beregningAktiviteter);
        beregningsgrunnlagRepository.lagre(behandling.getId(), builder, BeregningsgrunnlagTilstand.OPPRETTET);
    }
}
