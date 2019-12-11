package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningsgrunnlagFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelMerknad;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.vurder.RegelVurderBeregningsgrunnlag;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.fpsak.nare.evaluation.Evaluation;

@ApplicationScoped
public class VurderBeregningsgrunnlagTjeneste {

    private MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel;
    private MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel;

    VurderBeregningsgrunnlagTjeneste() {
        //for CDI proxy
    }

    @Inject
    public VurderBeregningsgrunnlagTjeneste(MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel,
                                            MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel) {
        this.oversetterTilRegel = oversetterTilRegel;
        this.oversetterFraRegel = oversetterFraRegel;
    }

    public BeregningsgrunnlagRegelResultat vurderBeregningsgrunnlag(BeregningsgrunnlagInput input, BeregningsgrunnlagGrunnlagEntitet oppdatertGrunnlag) {
        // Oversetter foreslått Beregningsgrunnlag -> regelmodell
        var beregningsgrunnlagRegel = oversetterTilRegel.map(input, oppdatertGrunnlag);

        String jsonInput = toJson(beregningsgrunnlagRegel);
        // Evaluerer hver BeregningsgrunnlagPeriode fra foreslått Beregningsgrunnlag
        List<RegelResultat> regelResultater = new ArrayList<>();
        for (BeregningsgrunnlagPeriode periode : beregningsgrunnlagRegel.getBeregningsgrunnlagPerioder()) {
            RegelVurderBeregningsgrunnlag regel = new RegelVurderBeregningsgrunnlag(periode);
            Evaluation evaluation = regel.evaluer(periode);
            regelResultater.add(RegelmodellOversetter.getRegelResultat(evaluation, jsonInput));
        }
        BeregningsgrunnlagEntitet beregningsgrunnlag = oversetterFraRegel.mapVurdertBeregningsgrunnlag(regelResultater, oppdatertGrunnlag.getBeregningsgrunnlag().orElse(null));
        List<BeregningAksjonspunktResultat> aksjonspunkter = Collections.emptyList();
        boolean vilkårOppfylt = erVilkårOppfylt(regelResultater);
        BeregningsgrunnlagRegelResultat beregningsgrunnlagRegelResultat = new BeregningsgrunnlagRegelResultat(beregningsgrunnlag, aksjonspunkter);
        beregningsgrunnlagRegelResultat.setVilkårOppfylt(vilkårOppfylt);
        return beregningsgrunnlagRegelResultat;
    }

    private boolean erVilkårOppfylt(List<RegelResultat> regelResultater) {
        return regelResultater.stream()
            .flatMap(regelResultat -> regelResultat.getMerknader().stream())
            .map(RegelMerknad::getMerknadKode)
            .noneMatch(avslagskode -> avslagskode.equals(Avslagsårsak.FOR_LAVT_BEREGNINGSGRUNNLAG.getKode()));
    }

    private static String toJson(no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.resultat.Beregningsgrunnlag beregningsgrunnlagRegel) {
        return JacksonJsonConfig.toJson(beregningsgrunnlagRegel, BeregningsgrunnlagFeil.FEILFACTORY::kanIkkeSerialisereRegelinput);
    }
}
