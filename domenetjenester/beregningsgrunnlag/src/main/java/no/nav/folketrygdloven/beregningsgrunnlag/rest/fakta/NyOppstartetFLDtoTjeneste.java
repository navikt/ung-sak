package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@ApplicationScoped
public class NyOppstartetFLDtoTjeneste implements FaktaOmBeregningTilfelleDtoTjeneste {

    private FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste;

    public NyOppstartetFLDtoTjeneste() {
        // For CDI
    }

    @Inject
    public NyOppstartetFLDtoTjeneste(FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste) {
        this.faktaOmBeregningAndelDtoTjeneste = faktaOmBeregningAndelDtoTjeneste;
    }

    @Override
    public void lagDto(BeregningsgrunnlagInput input, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt, FaktaOmBeregningDto faktaOmBeregningDto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = input.getBeregningsgrunnlag();
        List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        if (!tilfeller.contains(FaktaOmBeregningTilfelle.VURDER_NYOPPSTARTET_FL)  || faktaOmBeregningDto.getFrilansAndel() != null) {
            return;
        }
        faktaOmBeregningAndelDtoTjeneste.lagFrilansAndelDto(beregningsgrunnlag, input.getIayGrunnlag()).ifPresent(faktaOmBeregningDto::setFrilansAndel);
    }
}
