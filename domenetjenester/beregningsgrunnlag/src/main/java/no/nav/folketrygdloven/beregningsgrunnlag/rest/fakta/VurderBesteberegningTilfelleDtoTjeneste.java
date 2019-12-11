package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.VurderBesteberegningDto;

@ApplicationScoped
public class VurderBesteberegningTilfelleDtoTjeneste implements FaktaOmBeregningTilfelleDtoTjeneste {


    VurderBesteberegningTilfelleDtoTjeneste() {
        // For CDI
    }

    @Override
    public void lagDto(BeregningsgrunnlagInput input, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt, FaktaOmBeregningDto faktaOmBeregningDto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = input.getBeregningsgrunnlag();
        Optional<BeregningsgrunnlagEntitet> forrigeBeregningsgrunnlagOpt = forrigeGrunnlagOpt
            .flatMap(BeregningsgrunnlagGrunnlagEntitet::getBeregningsgrunnlag);
        if (!harBgTilfelle(beregningsgrunnlag)) {
            return;
        }
        if (forrigeBeregningsgrunnlagOpt.isPresent() && harBgTilfelle(forrigeBeregningsgrunnlagOpt.get())) {
            settVerdierNårUtførtTidligere(forrigeBeregningsgrunnlagOpt.get(), faktaOmBeregningDto);
        } else {
            settVerdierForFørsteGang(faktaOmBeregningDto);
        }
    }

    private void settVerdierForFørsteGang(FaktaOmBeregningDto faktaOmBeregningDto) {
        VurderBesteberegningDto vurderBesteberegning = new VurderBesteberegningDto();
        faktaOmBeregningDto.setVurderBesteberegning(vurderBesteberegning);
    }

    private void settVerdierNårUtførtTidligere(BeregningsgrunnlagEntitet forrigeBG, FaktaOmBeregningDto faktaOmBeregningDto) {
        VurderBesteberegningDto vurderBesteberegning = new VurderBesteberegningDto();
        vurderBesteberegning.setSkalHaBesteberegning(harBesteberegning(forrigeBG));
        faktaOmBeregningDto.setVurderBesteberegning(vurderBesteberegning);
    }

    private boolean harBgTilfelle(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING)
            || beregningsgrunnlag.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.FASTSETT_BESTEBEREGNING_FØDENDE_KVINNE);
    }

    private boolean harBesteberegning(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().stream()
            .flatMap(periode -> periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()).anyMatch(andel -> andel.getBesteberegningPrÅr() != null);
    }
}
