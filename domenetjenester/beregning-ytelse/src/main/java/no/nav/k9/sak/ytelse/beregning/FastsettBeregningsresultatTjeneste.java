package no.nav.k9.sak.ytelse.beregning;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFraRegelTilVL;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFraVLTilRegel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.beregning.regler.RegelFastsettBeregningsresultat;

@ApplicationScoped
public class FastsettBeregningsresultatTjeneste {

    private MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel;
    private MapBeregningsresultatFraRegelTilVL mapBeregningsresultatFraRegelTilVL;

    FastsettBeregningsresultatTjeneste() {
        //NOSONAR
    }

    @Inject
    public FastsettBeregningsresultatTjeneste(MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel,
                                              MapBeregningsresultatFraRegelTilVL mapBeregningsresultatFraRegelTilVL) {
        this.mapBeregningsresultatFraVLTilRegel = mapBeregningsresultatFraVLTilRegel;
        this.mapBeregningsresultatFraRegelTilVL = mapBeregningsresultatFraRegelTilVL;
    }

    public BeregningsresultatEntitet fastsettBeregningsresultat(List<Beregningsgrunnlag> beregningsgrunnlag, UttakResultat input) {
        return fastsettBeregningsresultat(beregningsgrunnlag, input, false);
    }

    public BeregningsresultatEntitet fastsettBeregningsresultat(List<Beregningsgrunnlag> beregningsgrunnlag, UttakResultat input, boolean skalVurdereOmArbeidsforholdGjelderFor) {
        // Map til regelmodell
        var regelmodell = mapBeregningsresultatFraVLTilRegel.mapFra(beregningsgrunnlag, input, skalVurdereOmArbeidsforholdGjelderFor);
        // Kalle regel
        var regel = new RegelFastsettBeregningsresultat();
        var outputContainer = no.nav.k9.sak.ytelse.beregning.regelmodell.Beregningsresultat.builder().build();
        Evaluation evaluation = regel.evaluer(regelmodell, outputContainer);
        String sporing = EvaluationSerializer.asJson(evaluation);

        // Map tilbake til domenemodell fra regelmodell
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput(toJson(regelmodell))
            .medRegelSporing(sporing)
            .build();

        mapBeregningsresultatFraRegelTilVL.mapFra(outputContainer, beregningsresultat);

        return beregningsresultat;
    }

    private String toJson(BeregningsresultatRegelmodell grunnlag) {
        return JacksonJsonConfig.toJson(grunnlag);
    }
}
