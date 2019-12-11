package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.BGAndelArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;

class KopierBeregningsgrunnlag {

    private KopierBeregningsgrunnlag() {
        // skjuler default public constructor
    }

    static BeregningsgrunnlagEntitet kopierVerdier(BeregningsgrunnlagEntitet gammelBG, BeregningsgrunnlagEntitet nyBG) {
        List<BeregningsgrunnlagPeriode> gamlePerioder = gammelBG.getBeregningsgrunnlagPerioder();
        nyBG.getBeregningsgrunnlagPerioder().forEach(nyPeriode -> kopierOverstyrteVerdierFraPeriode(gamlePerioder, nyPeriode));

        return nyBG;
    }

    private static void kopierOverstyrteVerdierFraPeriode(List<BeregningsgrunnlagPeriode> gamlePerioder, BeregningsgrunnlagPeriode nyPeriode) {
        Set<BeregningsgrunnlagPeriode> matchendePerioder = gamlePerioder.stream()
            .filter(gammelPeriode -> gammelPeriode.getPeriode().overlapper(nyPeriode.getPeriode()))
            .collect(Collectors.toSet());
        if (matchendePerioder.size() != 1) {
            return;
        }
        BeregningsgrunnlagPeriode gammelPeriode = matchendePerioder.iterator().next();
        List<BeregningsgrunnlagPrStatusOgAndel> gamleAndeler = gammelPeriode.getBeregningsgrunnlagPrStatusOgAndelList();
        nyPeriode.getBeregningsgrunnlagPrStatusOgAndelList().forEach(nyAndel -> kopierSaksbehandledeVerdierForKunYtelse(gamleAndeler, nyAndel));
        leggTilAndelerLagtTilAvSaksbehandler(nyPeriode, gamleAndeler);
    }

    private static void leggTilAndelerLagtTilAvSaksbehandler(BeregningsgrunnlagPeriode nyPeriode, List<BeregningsgrunnlagPrStatusOgAndel> gamleAndeler) {
        gamleAndeler.stream().filter(BeregningsgrunnlagPrStatusOgAndel::getLagtTilAvSaksbehandler)
        .forEach(gammelAndel -> BeregningsgrunnlagPrStatusOgAndel.builder(Kopimaskin.deepCopy(gammelAndel)).build(nyPeriode)
        );
    }

    private static void kopierSaksbehandledeVerdierForKunYtelse(List<BeregningsgrunnlagPrStatusOgAndel> gamleAndeler,
                                                                BeregningsgrunnlagPrStatusOgAndel nyAndel) {
        Set<BeregningsgrunnlagPrStatusOgAndel> matchendeAndeler = gamleAndeler.stream()
            .filter(gammelAndel -> nyAndel.getAndelsnr().equals(gammelAndel.getAndelsnr()))
            .collect(Collectors.toSet());
        if (matchendeAndeler.size() != 1) {
            throw new IllegalStateException("Kan ikke kopiere fra gammel behandling: Flere andeler som matcher på andelsnr i forrige behandling.");
        }
        BeregningsgrunnlagPrStatusOgAndel gammelAndel = matchendeAndeler.iterator().next();
        BeregningsgrunnlagPrStatusOgAndel.Builder builder = BeregningsgrunnlagPrStatusOgAndel.builder(nyAndel)
            .medBeregnetPrÅr(gammelAndel.getBeregnetPrÅr())
            .medNyIArbeidslivet(gammelAndel.getNyIArbeidslivet())
            .medFastsattAvSaksbehandler(gammelAndel.getFastsattAvSaksbehandler())
            .medLagtTilAvSaksbehandler(gammelAndel.getLagtTilAvSaksbehandler())
            .medInntektskategori(gammelAndel.getInntektskategori())
            .medBesteberegningPrÅr(gammelAndel.getBesteberegningPrÅr());
        if (erArbeidsforholdPåGammelAndelOverstyrt(gammelAndel)) {
            builder
                .medBGAndelArbeidsforhold(kopierBGAndelArbeidsforhold(nyAndel, gammelAndel));
        }
    }

    private static boolean erArbeidsforholdPåGammelAndelOverstyrt(BeregningsgrunnlagPrStatusOgAndel gammelAndel) {
        return gammelAndel.getBgAndelArbeidsforhold()
            .filter(bga -> bga.getErTidsbegrensetArbeidsforhold() != null || bga.erLønnsendringIBeregningsperioden() != null).isPresent();
    }

    private static BGAndelArbeidsforhold.Builder kopierBGAndelArbeidsforhold(BeregningsgrunnlagPrStatusOgAndel nyAndel, BeregningsgrunnlagPrStatusOgAndel gammelAndel) {
                return BGAndelArbeidsforhold.builder(nyAndel.getBgAndelArbeidsforhold().orElse(null))
                    .medTidsbegrensetArbeidsforhold(gammelAndel.getBgAndelArbeidsforhold()
                        .map(BGAndelArbeidsforhold::getErTidsbegrensetArbeidsforhold)
                        .orElse(null))
                    .medLønnsendringIBeregningsperioden(gammelAndel.getBgAndelArbeidsforhold()
                        .map(BGAndelArbeidsforhold::erLønnsendringIBeregningsperioden)
                        .orElse(null));

    }
}
