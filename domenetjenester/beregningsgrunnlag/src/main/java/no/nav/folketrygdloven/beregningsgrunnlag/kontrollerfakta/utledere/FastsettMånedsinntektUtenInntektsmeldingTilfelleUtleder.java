package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.utledere;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Arbeidsgiver;
import no.nav.foreldrepenger.behandlingslager.virksomhet.Organisasjonstype;

@ApplicationScoped
public class FastsettMånedsinntektUtenInntektsmeldingTilfelleUtleder implements TilfelleUtleder {


    public FastsettMånedsinntektUtenInntektsmeldingTilfelleUtleder() {
        // For CDI
    }

    @Override
    public Optional<FaktaOmBeregningTilfelle> utled(BeregningsgrunnlagInput input,
                                                    BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag) {
        return utled(beregningsgrunnlagGrunnlag);
    }

    protected Optional<FaktaOmBeregningTilfelle> utled(BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag) {
        boolean harKunstigVirksomhet = beregningsgrunnlagGrunnlag.getBeregningsgrunnlag()
            .stream()
            .flatMap(bg -> bg.getBeregningsgrunnlagPerioder().stream())
            .flatMap(p -> p.getBeregningsgrunnlagPrStatusOgAndelList().stream())
            .anyMatch(this::harKunstigArbeidsforhold);
        return harKunstigVirksomhet ? Optional.of(FaktaOmBeregningTilfelle.FASTSETT_MÅNEDSLØNN_ARBEIDSTAKER_UTEN_INNTEKTSMELDING) : Optional.empty();
    }

    private boolean harKunstigArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel a) {
        if (!a.getBgAndelArbeidsforhold().isPresent()) {
            return false;
        }
        BGAndelArbeidsforhold bgAndelArbeidsforhold = a.getBgAndelArbeidsforhold().get();
        Arbeidsgiver arbeidsgiver = bgAndelArbeidsforhold.getArbeidsgiver();
        return arbeidsgiver.getErVirksomhet() && Organisasjonstype.erKunstig(arbeidsgiver.getOrgnr());
    }
}
