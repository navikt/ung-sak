package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.List;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningsgrunnlagFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag;

public abstract class FullføreBeregningsgrunnlag {

    private MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel;
    private MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel;

    protected FullføreBeregningsgrunnlag() {
        //for CDI proxy
    }

    public FullføreBeregningsgrunnlag(MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel,
                                      MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel) {
        this.oversetterTilRegel = oversetterTilRegel;
        this.oversetterFraRegel = oversetterFraRegel;
    }

    public BeregningsgrunnlagEntitet fullføreBeregningsgrunnlag(BeregningsgrunnlagInput input, BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        // Oversetter foreslått Beregningsgrunnlag -> regelmodell
        var beregningsgrunnlagRegel = oversetterTilRegel.map(input, grunnlag);

        // Evaluerer hver BeregningsgrunnlagPeriode fra foreslått Beregningsgrunnlag
        List<RegelResultat> regelResultater = evaluerRegelmodell(beregningsgrunnlagRegel, input);

        // Oversett endelig resultat av regelmodell til fastsatt Beregningsgrunnlag  (+ spore input -> evaluation)
        BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().orElse(null);
        BeregningsgrunnlagEntitet fastsattBeregningsgrunnlag = oversetterFraRegel.mapFastsettBeregningsgrunnlag(beregningsgrunnlagRegel, regelResultater, beregningsgrunnlag);
        BeregningsgrunnlagVerifiserer.verifiserFastsattBeregningsgrunnlag(fastsattBeregningsgrunnlag, input.getAktivitetGradering());
        return fastsattBeregningsgrunnlag;
    }

    protected abstract List<RegelResultat> evaluerRegelmodell(Beregningsgrunnlag beregningsgrunnlagRegel, BeregningsgrunnlagInput input);

    protected static String toJson(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag beregningsgrunnlagRegel) {
        return JacksonJsonConfig.toJson(beregningsgrunnlagRegel, BeregningsgrunnlagFeil.FEILFACTORY::kanIkkeSerialisereRegelinput);
    }
}
