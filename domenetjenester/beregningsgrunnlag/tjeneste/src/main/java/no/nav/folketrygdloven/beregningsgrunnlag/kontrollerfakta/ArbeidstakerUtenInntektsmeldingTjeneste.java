package no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingSomIkkeKommer;

public class ArbeidstakerUtenInntektsmeldingTjeneste {

    private ArbeidstakerUtenInntektsmeldingTjeneste() {
        // Hide constructor
    }

    public static Collection<BeregningsgrunnlagPrStatusOgAndel> finnArbeidstakerAndelerUtenInntektsmelding(BeregningsgrunnlagEntitet beregningsgrunnlag,
                                                                                                           InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        if (!harArbeidstakerandel(beregningsgrunnlag)) {
            return Collections.emptyList();
        }

        List<InntektsmeldingSomIkkeKommer> manglendeInntektsmeldinger = inntektArbeidYtelseGrunnlag.getInntektsmeldingerSomIkkeKommer();
        if (manglendeInntektsmeldinger.isEmpty()) {
            return Collections.emptyList();
        }
        List<BeregningsgrunnlagPrStatusOgAndel> andelerIFørstePeriode = beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0)
            .getBeregningsgrunnlagPrStatusOgAndelList();
        return finnAndelerSomManglerIM(andelerIFørstePeriode, manglendeInntektsmeldinger);
    }

    private static boolean harArbeidstakerandel(BeregningsgrunnlagEntitet beregningsgrunnlag) {
        return beregningsgrunnlag.getBeregningsgrunnlagPerioder().get(0).getBeregningsgrunnlagPrStatusOgAndelList().stream()
            .anyMatch(andel -> andel.getAktivitetStatus().erArbeidstaker());
    }

    private static Collection<BeregningsgrunnlagPrStatusOgAndel> finnAndelerSomManglerIM(Collection<BeregningsgrunnlagPrStatusOgAndel> andeler,
                                                                                         List<InntektsmeldingSomIkkeKommer> manglendeInntektsmeldinger) {
        return andeler.stream()
            .filter(a -> a.getBgAndelArbeidsforhold().isPresent())
            .filter(a -> matchAndelMedInntektsmeldingSomIkkeKommer(manglendeInntektsmeldinger, a))
            .collect(Collectors.toList());
    }

    private static boolean matchAndelMedInntektsmeldingSomIkkeKommer(List<InntektsmeldingSomIkkeKommer> manglendeInntektsmeldinger,
                                                                     BeregningsgrunnlagPrStatusOgAndel andel) {
        return manglendeInntektsmeldinger.stream()
            .anyMatch(im -> andel.gjelderSammeArbeidsforhold(im.getArbeidsgiver(), im.getRef()));
    }
}
