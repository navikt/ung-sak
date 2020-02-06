package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaOmBeregningAndelDto;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.FaktaOmBeregningDto;

@ApplicationScoped
public class VurderLønnsendringDtoTjeneste implements FaktaOmBeregningTilfelleDtoTjeneste {

    private FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste;

    public VurderLønnsendringDtoTjeneste() {
        // For CDI
    }

    @Inject
    public VurderLønnsendringDtoTjeneste(FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste) {
        this.faktaOmBeregningAndelDtoTjeneste = faktaOmBeregningAndelDtoTjeneste;
    }

    @Override
    public void lagDto(BeregningsgrunnlagInput input, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt, FaktaOmBeregningDto faktaOmBeregningDto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = input.getBeregningsgrunnlag();
        List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        if (tilfeller.contains(FaktaOmBeregningTilfelle.VURDER_LØNNSENDRING)) {
            var ref = input.getBehandlingReferanse();
            List<FaktaOmBeregningAndelDto> arbeidsforholdUtenInntektsmeldingDtoList = faktaOmBeregningAndelDtoTjeneste.lagArbeidsforholdUtenInntektsmeldingDtoList(ref.getAktørId(), beregningsgrunnlag, input.getIayGrunnlag());
            if (!arbeidsforholdUtenInntektsmeldingDtoList.isEmpty()) {
                faktaOmBeregningDto.setArbeidsforholdMedLønnsendringUtenIM(arbeidsforholdUtenInntektsmeldingDtoList);
            }
        }
    }
}
