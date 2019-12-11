package no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling;

import no.nav.folketrygdloven.beregningsgrunnlag.RepositoryProvider;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagRepository;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagTilstand;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

public class BeregningsgrunnlagScenario implements TestScenarioTillegg {

    private BeregningsgrunnlagEntitet.Builder beregningsgrunnlagBuilder;

    BeregningsgrunnlagScenario() {
        this.beregningsgrunnlagBuilder = BeregningsgrunnlagEntitet.builder();
    }

    BeregningsgrunnlagEntitet.Builder getBeregningsgrunnlagBuilder() {
        return beregningsgrunnlagBuilder;
    }

    @Override
    public void lagre(Behandling behandling, RepositoryProvider repositoryProvider) {
        BeregningsgrunnlagRepository beregningsgrunnlagRepository = repositoryProvider.getBeregningsgrunnlagRepository();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagBuilder.build();
        beregningsgrunnlagRepository.lagre(behandling.getId(), beregningsgrunnlag, BeregningsgrunnlagTilstand.OPPRETTET);
    }
}
