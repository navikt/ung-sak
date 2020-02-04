package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.KortvarigArbeidsforholdTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.BeregningsgrunnlagArbeidsforholdDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.FaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.rest.dto.KortvarigeArbeidsforholdDto;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;

@ApplicationScoped
class KortvarigeArbeidsforholdDtoTjeneste implements FaktaOmBeregningTilfelleDtoTjeneste {

    private BeregningsgrunnlagDtoUtil dtoUtil;

    public KortvarigeArbeidsforholdDtoTjeneste() {
        // For CDI
    }

    @Inject
    public KortvarigeArbeidsforholdDtoTjeneste(BeregningsgrunnlagDtoUtil dtoUtil) {
        this.dtoUtil = dtoUtil;
    }

    @Override
    public void lagDto(BeregningsgrunnlagInput input,
                       Optional<BeregningsgrunnlagGrunnlagEntitet> forrigeGrunnlagOpt,
                       FaktaOmBeregningDto faktaOmBeregningDto) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = input.getBeregningsgrunnlag();
        if (!beregningsgrunnlag.getFaktaOmBeregningTilfeller().contains(FaktaOmBeregningTilfelle.VURDER_TIDSBEGRENSET_ARBEIDSFORHOLD)) {
            return;
        }
        List<KortvarigeArbeidsforholdDto> arbeidsforholdDto = lagKortvarigeArbeidsforholdDto(input.getBehandlingReferanse(), beregningsgrunnlag, input.getIayGrunnlag());
        faktaOmBeregningDto.setKortvarigeArbeidsforhold(arbeidsforholdDto);
    }

    private List<KortvarigeArbeidsforholdDto> lagKortvarigeArbeidsforholdDto(BehandlingReferanse ref, BeregningsgrunnlagEntitet beregningsgrunnlag, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarige = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(ref.getAktørId(), beregningsgrunnlag, inntektArbeidYtelseGrunnlag);
        return kortvarige.entrySet().stream()
            .map(entry -> mapFraYrkesaktivitet(entry.getKey(), inntektArbeidYtelseGrunnlag))
            .collect(Collectors.toList());
    }

    private KortvarigeArbeidsforholdDto mapFraYrkesaktivitet(BeregningsgrunnlagPrStatusOgAndel prStatusOgAndel, InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        KortvarigeArbeidsforholdDto beregningArbeidsforhold = new KortvarigeArbeidsforholdDto();
        beregningArbeidsforhold.setErTidsbegrensetArbeidsforhold(prStatusOgAndel.getBgAndelArbeidsforhold().map(BGAndelArbeidsforhold::getErTidsbegrensetArbeidsforhold).orElse(null));
        beregningArbeidsforhold.setAndelsnr(prStatusOgAndel.getAndelsnr());
        Optional<BeregningsgrunnlagArbeidsforholdDto> arbDto = dtoUtil.lagArbeidsforholdDto(prStatusOgAndel, Optional.empty(), inntektArbeidYtelseGrunnlag);
        arbDto.ifPresent(beregningArbeidsforhold::setArbeidsforhold);
        return beregningArbeidsforhold;
    }
}
