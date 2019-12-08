package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningsgrunnlagFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelmodellOversetter;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fordel.RegelFordelBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.fpsak.nare.evaluation.Evaluation;

@ApplicationScoped
public class FordelBeregningsgrunnlagTjeneste {


    private FastsettBeregningsgrunnlagPerioderTjeneste fastsettBeregningsgrunnlagPerioderTjeneste;
    private MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel;
    private MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel;


    public FordelBeregningsgrunnlagTjeneste() {
        // CDI
    }

    @Inject
    public FordelBeregningsgrunnlagTjeneste(FastsettBeregningsgrunnlagPerioderTjeneste fastsettBeregningsgrunnlagPerioderTjeneste, MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel, MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel) {
        this.fastsettBeregningsgrunnlagPerioderTjeneste = fastsettBeregningsgrunnlagPerioderTjeneste;
        this.oversetterFraRegel = oversetterFraRegel;
        this.oversetterTilRegel = oversetterTilRegel;
    }

    public BeregningsgrunnlagEntitet fordelBeregningsgrunnlag(BeregningsgrunnlagInput input, BeregningsgrunnlagEntitet beregningsgrunnlag) {
        BeregningsgrunnlagEntitet bgMedRefusjon = fastsettBeregningsgrunnlagPerioderTjeneste
            .fastsettPerioderForRefusjonOgGradering(input, beregningsgrunnlag);
        var ref = input.getBehandlingReferanse();
        return kjørRegelForOmfordeling(ref, bgMedRefusjon);
    }

    private BeregningsgrunnlagEntitet kjørRegelForOmfordeling(BehandlingReferanse ref, BeregningsgrunnlagEntitet beregningsgrunnlagEntitet) {
        var regelPerioder = oversetterTilRegel.mapTilFordelingsregel(ref, beregningsgrunnlagEntitet);
        String input = toJson(regelPerioder);
        List<RegelResultat> regelResultater = new ArrayList<>();
        for (BeregningsgrunnlagPeriode periode : regelPerioder) {
            RegelFordelBeregningsgrunnlag regel = new RegelFordelBeregningsgrunnlag(periode);
            Evaluation evaluation = regel.evaluer(periode);
            regelResultater.add(RegelmodellOversetter.getRegelResultat(evaluation, input));
        }
        return oversetterFraRegel.mapForFordel(regelPerioder, regelResultater, beregningsgrunnlagEntitet);
    }

    private static String toJson(List<no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode> regelPerioder) {
        return JacksonJsonConfig.toJson(regelPerioder, BeregningsgrunnlagFeil.FEILFACTORY::kanIkkeSerialisereRegelinput);
    }

}
