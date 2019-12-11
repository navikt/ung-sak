package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.VurderMilitærTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;

@ApplicationScoped
public class VurderMilitærTilfelleUtleder implements TilfelleUtleder {

    private VurderMilitærTjeneste vurderMiliærTjeneste;

    VurderMilitærTilfelleUtleder() {
        // For CDI
    }

    @Inject
    public VurderMilitærTilfelleUtleder(VurderMilitærTjeneste vurderMilitærTjeneste) {
        this.vurderMiliærTjeneste = vurderMilitærTjeneste;
    }

    @Override
    public Optional<FaktaOmBeregningTilfelle> utled(BeregningsgrunnlagInput input, BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag) {
        var ref = input.getBehandlingReferanse();
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagGrunnlag.getBeregningsgrunnlag().orElse(null);
        Objects.requireNonNull(beregningsgrunnlag, "beregningsgrunnlag");
        return vurderMiliærTjeneste.harOppgittMilitærIOpptjeningsperioden(ref, input.getIayGrunnlag()) ?
            Optional.of(FaktaOmBeregningTilfelle.VURDER_MILITÆR_SIVILTJENESTE) : Optional.empty();
    }
}
