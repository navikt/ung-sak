package no.nav.folketrygdloven.beregningsgrunnlag.rest.fakta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektFilter;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.k9.kodeverk.beregningsgrunnlag.FaktaOmBeregningTilfelle;
import no.nav.k9.kodeverk.iay.AktivitetStatus;

class FinnInntektForVisning {

    private static final BigDecimal MND_I_1_ÅR = BigDecimal.valueOf(12);

    private FinnInntektForVisning() {
        // Hide constructor
    }

    static BigDecimal finnInntektForPreutfylling(BeregningsgrunnlagPrStatusOgAndel andel) {
        if (andel.getBesteberegningPrÅr() != null) {
            return andel.getBesteberegningPrÅr().divide(MND_I_1_ÅR, 10, RoundingMode.HALF_EVEN);
        }
        return andel.getBeregnetPrÅr() == null ? null : andel.getBeregnetPrÅr().divide(MND_I_1_ÅR, 10, RoundingMode.HALF_EVEN);
    }

    static Optional<BigDecimal> finnInntektForKunLese(BehandlingReferanse ref,
                                                      BeregningsgrunnlagPrStatusOgAndel andel,
                                                      Optional<Inntektsmelding> inntektsmeldingForAndel,
                                                      InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        List<FaktaOmBeregningTilfelle> tilfeller = andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag().getFaktaOmBeregningTilfeller();
        if (tilfeller.contains(FaktaOmBeregningTilfelle.VURDER_AT_OG_FL_I_SAMME_ORGANISASJON)) {
            if (andel.getAktivitetStatus().erFrilanser()) {
                return Optional.empty();
            }
            if (andel.getAktivitetStatus().erArbeidstaker()) {
                if (!inntektsmeldingForAndel.isPresent()) {
                    return Optional.empty();
                }
            }
        }
        if (andel.getAktivitetStatus().erArbeidstaker()) {
            return finnInntektsbeløpForArbeidstaker(ref, andel, inntektsmeldingForAndel, inntektArbeidYtelseGrunnlag);
        }
        if (andel.getAktivitetStatus().erFrilanser()) {
            return finnMånedsbeløpIBeregningsperiodenForFrilanser(ref, andel, inntektArbeidYtelseGrunnlag);
        }
        if (andel.getAktivitetStatus().equals(AktivitetStatus.DAGPENGER) || andel.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER)) {
            return FinnInntektFraYtelse.finnÅrbeløpFraMeldekort(ref, andel.getAktivitetStatus(), inntektArbeidYtelseGrunnlag)
                .map(årsbeløp -> årsbeløp.divide(MND_I_1_ÅR, 10, RoundingMode.HALF_EVEN));
        }
        return Optional.empty();
    }

    private static Optional<BigDecimal> finnInntektsbeløpForArbeidstaker(BehandlingReferanse ref, BeregningsgrunnlagPrStatusOgAndel andel,
                                                                         Optional<Inntektsmelding> inntektsmeldingForAndel,
                                                                         InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        Optional<BigDecimal> inntektsmeldingBeløp = inntektsmeldingForAndel
            .map(Inntektsmelding::getInntektBeløp)
            .map(Beløp::getVerdi);
        if (inntektsmeldingBeløp.isPresent()) {
            return inntektsmeldingBeløp;
        }
        return finnMånedsbeløpIBeregningsperiodenForArbeidstaker(ref, andel, inntektArbeidYtelseGrunnlag);
    }

    private static Optional<BigDecimal> finnMånedsbeløpIBeregningsperiodenForFrilanser(BehandlingReferanse ref, BeregningsgrunnlagPrStatusOgAndel andel,
                                                                                       InntektArbeidYtelseGrunnlag inntektArbeidYtelseGrunnlag) {
        return InntektForAndelTjeneste.finnSnittAvFrilansinntektIBeregningsperioden(ref.getAktørId(),
            inntektArbeidYtelseGrunnlag, andel, andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag().getSkjæringstidspunkt());
    }

    private static Optional<BigDecimal> finnMånedsbeløpIBeregningsperiodenForArbeidstaker(BehandlingReferanse ref, BeregningsgrunnlagPrStatusOgAndel andel,
                                                                                          InntektArbeidYtelseGrunnlag grunnlag) {
        return grunnlag.getAktørInntektFraRegister(ref.getAktørId())
            .map(aktørInntekt -> {
                var skjæringstidspunkt = andel.getBeregningsgrunnlagPeriode().getBeregningsgrunnlag().getSkjæringstidspunkt();
                var filter = new InntektFilter(aktørInntekt).før(skjæringstidspunkt);
                BigDecimal årsbeløp = InntektForAndelTjeneste.finnSnittinntektPrÅrForArbeidstakerIBeregningsperioden(filter, andel);
                return årsbeløp.divide(MND_I_1_ÅR, 10, RoundingMode.HALF_EVEN);
            });
    }

}
