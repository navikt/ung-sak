package no.nav.folketrygdloven.beregningsgrunnlag;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AndelGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;
import no.nav.foreldrepenger.domene.iay.modell.Refusjon;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.tid.ÅpenDatoIntervallEntitet;

public class BeregningInntektsmeldingTjeneste {

    private static final int MND_I_1_ÅR = 12;
    private static final int SEKS = 6;

    private BeregningInntektsmeldingTjeneste() {
    }

    public static boolean erTotaltRefusjonskravStørreEnnEllerLikSeksG(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, Collection<Inntektsmelding> inntektsmeldinger) {
        Beløp grunnbeløp = beregningsgrunnlagPeriode.getBeregningsgrunnlag().getGrunnbeløp();
        Beløp seksG = grunnbeløp.multipliser(SEKS);
        Beløp totaltRefusjonskravPrÅr = new Beløp(beregnTotaltRefusjonskravPrÅrIPeriode(beregningsgrunnlagPeriode, inntektsmeldinger));
        return totaltRefusjonskravPrÅr.compareTo(seksG) >= 0;
    }

    private static BigDecimal beregnTotaltRefusjonskravPrÅrIPeriode(BeregningsgrunnlagPeriode beregningsgrunnlagPeriode, Collection<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .map(im -> finnRefusjonskravIPeriode(im, beregningsgrunnlagPeriode.getPeriode()))
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .multiply(BigDecimal.valueOf(MND_I_1_ÅR));
    }

    private static BigDecimal finnRefusjonskravIPeriode(Inntektsmelding inntektsmelding, ÅpenDatoIntervallEntitet periode) {
        if (inntektsmelding.getRefusjonOpphører() != null && inntektsmelding.getRefusjonOpphører().isAfter(periode.getFomDato())) {
            return inntektsmelding.getRefusjonBeløpPerMnd().getVerdi();
        }
        return inntektsmelding.getEndringerRefusjon().stream()
            .filter(r -> periode.inkluderer(r.getFom()))
            .findFirst().map(Refusjon::getRefusjonsbeløp).orElse(Beløp.ZERO).getVerdi();
    }

    public static Optional<BigDecimal> finnRefusjonskravPrÅrIPeriodeForAndel(BeregningsgrunnlagPrStatusOgAndel andel, ÅpenDatoIntervallEntitet periode, Collection<Inntektsmelding> inntektsmeldinger) {
        return finnInntektsmeldingForAndel(andel, inntektsmeldinger).map(im -> finnRefusjonskravIPeriode(im, periode)).map(ref -> ref.multiply(BigDecimal.valueOf(MND_I_1_ÅR)));
    }

    public static Optional<Inntektsmelding> finnInntektsmeldingForAndel(BeregningsgrunnlagPrStatusOgAndel andel, Collection<Inntektsmelding> inntektsmeldinger) {
        return inntektsmeldinger.stream()
            .filter(im -> andel.gjelderInntektsmeldingFor(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .findFirst();
    }

    public static Optional<AndelGradering> finnGraderingForAndel(BeregningsgrunnlagPrStatusOgAndel andel, AktivitetGradering aktivitetGradering) {
        return aktivitetGradering.getAndelGradering().stream()
            .filter(andelGradering -> andelGradering.matcher(andel))
            .findFirst();
    }
}
