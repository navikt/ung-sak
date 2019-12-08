package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.tilfeller;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.FaktaOmBeregningTilfelleRef;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FaktaBeregningLagreDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderTidsbegrensetArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.VurderteArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
@FaktaOmBeregningTilfelleRef("VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD")
public class VurderTidsbegrensetArbeidsforholdOppdaterer implements FaktaOmBeregningTilfelleOppdaterer {

    @Override
    public void oppdater(FaktaBeregningLagreDto dto, BehandlingReferanse behandlingReferanse, BeregningsgrunnlagEntitet nyttBeregningsgrunnlag, Optional<BeregningsgrunnlagEntitet> forrigeBg) {
        VurderTidsbegrensetArbeidsforholdDto tidsbegrensetDto = dto.getVurderTidsbegrensetArbeidsforhold();
        BeregningsgrunnlagPeriode periode = nyttBeregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0);
        List<VurderteArbeidsforholdDto> fastsatteArbeidsforhold = tidsbegrensetDto.getFastsatteArbeidsforhold();
        for (VurderteArbeidsforholdDto arbeidsforhold : fastsatteArbeidsforhold) {
            BeregningsgrunnlagPrStatusOgAndel korrektAndel = periode.getBeregningsgrunnlagPrStatusOgAndelList().stream()
                .filter(a -> a.getAndelsnr().equals(arbeidsforhold.getAndelsnr()))
                .findFirst()
                .orElseThrow(() -> VurderTidsbegrensetArbeidsforholdOppdatererFeil.FACTORY.finnerIkkeAndelFeil().toException());
            BGAndelArbeidsforhold.Builder bgAndelArbeidsforhold = BGAndelArbeidsforhold
                .builder(korrektAndel.getBgAndelArbeidsforhold().orElse(null))
                .medTidsbegrensetArbeidsforhold(arbeidsforhold.isTidsbegrensetArbeidsforhold());
            BeregningsgrunnlagPrStatusOgAndel
                .builder(korrektAndel)
                .medBGAndelArbeidsforhold(bgAndelArbeidsforhold);
        }
    }

    private interface VurderTidsbegrensetArbeidsforholdOppdatererFeil extends DeklarerteFeil {

        VurderTidsbegrensetArbeidsforholdOppdatererFeil FACTORY = FeilFactory.create(VurderTidsbegrensetArbeidsforholdOppdatererFeil.class);

        @TekniskFeil(feilkode = "FP-238175", feilmelding = "Finner ikke andelen for eksisterende grunnlag", logLevel = LogLevel.WARN)
        Feil finnerIkkeAndelFeil();
    }


}
