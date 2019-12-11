package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.ATogFLISammeOrganisasjonDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;

@ApplicationScoped
public class VurderATFLISammeOrgDtoTjeneste implements FaktaOmBeregningTilfelleDtoTjeneste {

    private FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste;


    public VurderATFLISammeOrgDtoTjeneste() {
        // For CDI
    }

    @Inject
    public VurderATFLISammeOrgDtoTjeneste(FaktaOmBeregningAndelDtoTjeneste faktaOmBeregningAndelDtoTjeneste) {
        this.faktaOmBeregningAndelDtoTjeneste = faktaOmBeregningAndelDtoTjeneste;
    }

    @Override
    public void lagDto(BeregningsgrunnlagInput input, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt, FaktaOmBeregningDto faktaOmBeregningDto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = input.getBeregningsgrunnlag();
        List<FaktaOmBeregningTilfelle> tilfeller = beregningsgrunnlag.getFaktaOmBeregningTilfeller();
        if (tilfeller.contains(FaktaOmBeregningTilfelle.VURDER_AT_OG_FL_I_SAMME_ORGANISASJON)) {
            var ref = input.getBehandlingReferanse();
            List<ATogFLISammeOrganisasjonDto> aTogFLISammeOrganisasjonDto = faktaOmBeregningAndelDtoTjeneste.lagATogFLISAmmeOrganisasjonListe(ref, beregningsgrunnlag, input.getInntektsmeldinger(), input.getIayGrunnlag());
            if (faktaOmBeregningDto.getFrilansAndel() == null) {
                faktaOmBeregningAndelDtoTjeneste.lagFrilansAndelDto(beregningsgrunnlag, input.getIayGrunnlag()).ifPresent(faktaOmBeregningDto::setFrilansAndel);
            }
            faktaOmBeregningDto.setArbeidstakerOgFrilanserISammeOrganisasjonListe(aTogFLISammeOrganisasjonDto);
        }
    }
}
