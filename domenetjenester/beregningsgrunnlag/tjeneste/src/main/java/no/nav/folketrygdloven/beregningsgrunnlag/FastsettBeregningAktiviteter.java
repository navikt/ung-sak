package no.nav.folketrygdloven.beregningsgrunnlag;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningAktiviteterFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningAktiviteterFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.regelmodell.AktivitetStatusModell;

public class FastsettBeregningAktiviteter {

    private MapBeregningAktiviteterFraVLTilRegel oversetterTilRegel;
    private MapBeregningAktiviteterFraRegelTilVL oversetterFraRegel;

    public FastsettBeregningAktiviteter() {
        this.oversetterTilRegel = new MapBeregningAktiviteterFraVLTilRegel();
        this.oversetterFraRegel = new MapBeregningAktiviteterFraRegelTilVL();
    }

    public BeregningAktivitetAggregatEntitet fastsettAktiviteter(BeregningsgrunnlagInput input) {
        // Oversetter Opptjening -> regelmodell, hvor også skjæringstidspunkt for Opptjening er lagret
        AktivitetStatusModell regelmodell = oversetterTilRegel.mapForSkjæringstidspunkt(input);
        return oversetterFraRegel.map(regelmodell);
    }

}
