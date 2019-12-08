package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fordel;

import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.Beregnet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.fpsak.nare.DynamicRuleService;
import no.nav.fpsak.nare.Ruleset;
import no.nav.fpsak.nare.specification.Specification;

public class RegelFordelBeregningsgrunnlag extends DynamicRuleService<BeregningsgrunnlagPeriode> {

    public static final String ID = "FP_BR 22.3";

    public RegelFordelBeregningsgrunnlag(BeregningsgrunnlagPeriode regelmodell) {
        super(regelmodell);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Specification<BeregningsgrunnlagPeriode> getSpecification() {
        Ruleset<BeregningsgrunnlagPeriode> rs = new Ruleset<>();

        Specification<BeregningsgrunnlagPeriode> fastsettFordelingAvBeregningsgrunnlag = new FastsettNyFordeling(regelmodell).getSpecification();

        Specification<BeregningsgrunnlagPeriode> overstigerTotalRefusjonBeregningsgrunnlag = rs.beregningHvisRegel(new SjekkOverstigerTotalRefusjonBeregningsgrunnlag(),
            new Beregnet(), fastsettFordelingAvBeregningsgrunnlag);

        Specification<BeregningsgrunnlagPeriode> sjekkRefusjonMotBeregningsgrunnlag = rs.beregningHvisRegel(new SjekkHarRefusjonSomOverstigerBeregningsgrunnlag(),
            overstigerTotalRefusjonBeregningsgrunnlag, new Beregnet());

        return sjekkRefusjonMotBeregningsgrunnlag;
    }
}
