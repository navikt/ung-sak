package no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt;

import java.math.BigDecimal;
import java.util.Optional;

import no.nav.folketrygdloven.beregningsgrunnlag.Kopimaskin;
import no.nav.folketrygdloven.beregningsgrunnlag.MatchBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.FastsatteVerdierDto;
import no.nav.folketrygdloven.beregningsgrunnlag.aksjonspunkt.dto.RedigerbarAndelFaktaOmBeregningDto;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.k9.kodeverk.iay.AktivitetStatus;
import no.nav.k9.kodeverk.iay.Inntektskategori;

public class FastsettBeregningVerdierTjeneste {

    private FastsettBeregningVerdierTjeneste() {
        // skjul
    }

    public static void fastsettVerdierForAndel(RedigerbarAndelFaktaOmBeregningDto andel, FastsatteVerdierDto fastsatteVerdier,
                                               BeregningsgrunnlagPeriode periode, Optional<BeregningsgrunnlagPeriode> periodeForrigeGrunnlag) {
        validerAtPåkrevdeVerdierErSatt(andel);
        if (andel.getNyAndel() || andel.getLagtTilAvSaksbehandler()) {
            if (andel.getAktivitetStatus().isPresent()) {
                fastsettBeløpForNyAndelMedAktivitetstatus(periode, andel.getAktivitetStatus().get(),
                    fastsatteVerdier);
            } else {
                fastsettBeløpForNyAndelFraAndelsreferanse(andel, periode, periodeForrigeGrunnlag, fastsatteVerdier);
            }
        } else {
            BeregningsgrunnlagPrStatusOgAndel korrektAndel = getKorrektAndel(periode, periodeForrigeGrunnlag, andel);
            settInntektskategoriOgFastsattBeløp(andel, fastsatteVerdier, korrektAndel, periode);
        }
    }

    private static void validerAtPåkrevdeVerdierErSatt(RedigerbarAndelFaktaOmBeregningDto andel) {
        if (andel.getAndelsnr().isEmpty() && andel.getAktivitetStatus().isEmpty()) {
            throw new IllegalArgumentException("Enten andelsnr eller aktivitetstatus må vere satt.");
        }
        if (andel.getAktivitetStatus().isPresent() && !andel.getNyAndel()) {
            throw new IllegalArgumentException("Kun nye andeler kan identifiseres med aktivitetstatus");
        }
        if (!andel.getLagtTilAvSaksbehandler() && !andel.getNyAndel() && andel.getAndelsnr().isEmpty()) {
            throw new IllegalArgumentException("Eksisterende andeler som ikkje er lagt til av saksbehandler må ha andelsnr.");
        }
    }

    private static void fastsettBeløpForNyAndelFraAndelsreferanse(RedigerbarAndelFaktaOmBeregningDto andel, BeregningsgrunnlagPeriode periode,
                                                                          Optional<BeregningsgrunnlagPeriode> periodeForrigeGrunnlag,
                                                                          FastsatteVerdierDto fastsatteVerdier) {
        if (andel.getAndelsnr().isEmpty()) {
            throw new IllegalStateException("Må ha andelsnr for å fastsette beløp fra andelsnr");
        }
        Long andelsnr = andel.getAndelsnr().get();
        BeregningsgrunnlagPrStatusOgAndel korrektAndel;
        if (!andel.getNyAndel() && periodeForrigeGrunnlag.isPresent()) {
            korrektAndel = Kopimaskin.deepCopy(MatchBeregningsgrunnlagTjeneste.matchMedAndelFraPeriodePåAndelsnr(periodeForrigeGrunnlag.get(), andelsnr));
        } else {
            korrektAndel = Kopimaskin.deepCopy((MatchBeregningsgrunnlagTjeneste.matchMedAndelFraPeriodePåAndelsnr(periode, andelsnr)));
        }
        settInntektskategoriOgFastsattBeløp(andel, fastsatteVerdier, korrektAndel, periode);
    }

    private static void settInntektskategoriOgFastsattBeløp(RedigerbarAndelFaktaOmBeregningDto andel, FastsatteVerdierDto fastsatteVerdier,
                                                                                  BeregningsgrunnlagPrStatusOgAndel korrektAndel,
                                                                                  BeregningsgrunnlagPeriode korrektPeriode) {
        Inntektskategori nyInntektskategori = fastsatteVerdier.getInntektskategori();
        BeregningsgrunnlagPrStatusOgAndel.Builder andelBuilder = BeregningsgrunnlagPrStatusOgAndel.builder(korrektAndel)
            .medInntektskategori(nyInntektskategori == null ? korrektAndel.getInntektskategori() : nyInntektskategori)
            .medBeregnetPrÅr(fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr())
            .medBesteberegningPrÅr(Boolean.TRUE.equals(fastsatteVerdier.getSkalHaBesteberegning()) ? fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr() : null)
            .medFastsattAvSaksbehandler(true);
        if (fastsatteVerdier.getRefusjonPrÅr() != null) {
            BGAndelArbeidsforhold.Builder builder = BGAndelArbeidsforhold
                .builder(korrektAndel.getBgAndelArbeidsforhold().orElse(null))
                .medRefusjonskravPrÅr(BigDecimal.valueOf(fastsatteVerdier.getRefusjonPrÅr()));
            andelBuilder.medBGAndelArbeidsforhold(builder);
        }
        if (andel.getNyAndel() || andel.getLagtTilAvSaksbehandler()) {
            andelBuilder.nyttAndelsnr(korrektPeriode).medLagtTilAvSaksbehandler(true).build(korrektPeriode);
        }
    }


    private static void fastsettBeløpForNyAndelMedAktivitetstatus(BeregningsgrunnlagPeriode periode,
                                                                                        AktivitetStatus aktivitetStatus,
                                                                                        FastsatteVerdierDto fastsatteVerdier) {
        BigDecimal fastsatt = fastsatteVerdier.finnEllerUtregnFastsattBeløpPrÅr();// NOSONAR
        Inntektskategori nyInntektskategori = fastsatteVerdier.getInntektskategori();
        if (nyInntektskategori == null) {
            throw new IllegalStateException("Kan ikke sette inntektskategori lik null på ny andel.");
        }
        BeregningsgrunnlagPrStatusOgAndel.builder()
            .medAktivitetStatus(aktivitetStatus)
            .medInntektskategori(nyInntektskategori)
            .medBeregnetPrÅr(fastsatt)
            .medBesteberegningPrÅr(Boolean.TRUE.equals(fastsatteVerdier.getSkalHaBesteberegning()) ? fastsatt : null)
            .medFastsattAvSaksbehandler(true)
            .medLagtTilAvSaksbehandler(true)
            .build(periode);
    }


    private static BeregningsgrunnlagPrStatusOgAndel getKorrektAndel(BeregningsgrunnlagPeriode periode,
                                                                     Optional<BeregningsgrunnlagPeriode> forrigePeriode,
                                                                     RedigerbarAndelFaktaOmBeregningDto andel) {
        if (andel.getAndelsnr().isEmpty()) {
            throw new IllegalArgumentException("Har ikke andelsnr når man burde ha hatt det.");
        }
        Long andelsnr = andel.getAndelsnr().get();
        if (andel.getLagtTilAvSaksbehandler() && !andel.getNyAndel() && forrigePeriode.isPresent()) {
            return MatchBeregningsgrunnlagTjeneste.matchMedAndelFraPeriodePåAndelsnr(forrigePeriode.get(), andelsnr);
        }
        return MatchBeregningsgrunnlagTjeneste.matchMedAndelFraPeriodePåAndelsnr(periode, andelsnr);
    }

}
