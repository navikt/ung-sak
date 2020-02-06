package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagAktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;
import no.nav.k9.kodeverk.arbeidsforhold.AktivitetStatus;
import no.nav.k9.sak.kontrakt.beregningsgrunnlag.aksjonspunkt.VurderMilitærDto;

@ApplicationScoped
public class VurderMilitærDtoTjeneste implements FaktaOmBeregningTilfelleDtoTjeneste {


    VurderMilitærDtoTjeneste() {
        // For CDI
    }

    @Override
    public void lagDto(BeregningsgrunnlagInput input, Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt, FaktaOmBeregningDto faktaOmBeregningDto) {
        if (forrigeGrunnlagOpt.isPresent() && forrigeGrunnlagOpt.get().getBeregningsgrunnlag().isPresent()) {
            BeregningsgrunnlagEntitet forrigeBG = forrigeGrunnlagOpt.get().getBeregningsgrunnlag().get();

            List<BeregningsgrunnlagAktivitetStatus> aktivitetStatuser = forrigeBG.getAktivitetStatuser();
            VurderMilitærDto dto = new VurderMilitærDto(aktivitetStatuser.stream().anyMatch(status -> status.getAktivitetStatus().equals(AktivitetStatus.MILITÆR_ELLER_SIVIL)));

            faktaOmBeregningDto.setVurderMilitaer(dto);
        }

    }
}
