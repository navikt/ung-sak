package no.nav.folketrygdloven.beregningsgrunnlag.ytelse.fp;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.FullføreBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.RegelmodellOversetter;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningsgrunnlagFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.fastsette.RegelFullføreBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.fpsak.nare.evaluation.Evaluation;

@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class FullføreBeregningsgrunnlagFPImpl extends FullføreBeregningsgrunnlag {

    FullføreBeregningsgrunnlagFPImpl() {
        //for CDI proxy
    }

    @Inject
    public FullføreBeregningsgrunnlagFPImpl(MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel,
                                            MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel) {
        super(oversetterFraRegel, oversetterTilRegel);
    }

    @Override
    protected List<RegelResultat> evaluerRegelmodell(Beregningsgrunnlag beregningsgrunnlagRegel, BeregningsgrunnlagInput bgInput) {
        List<RegelResultat> regelResultater = new ArrayList<>();
        String inputJson = toJson(beregningsgrunnlagRegel);
        for (BeregningsgrunnlagPeriode periode : beregningsgrunnlagRegel.getBeregningsgrunnlagPerioder()) {
            RegelFullføreBeregningsgrunnlag regel = new RegelFullføreBeregningsgrunnlag(periode);
            Evaluation evaluation = regel.evaluer(periode);
            regelResultater.add(RegelmodellOversetter.getRegelResultat(evaluation, inputJson));
        }
        return regelResultater;
    }
}
