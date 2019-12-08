package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.arbeidstaker;

import org.junit.Test;

import no.finn.unleash.FakeUnleash;
import no.finn.unleash.Unleash;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.avkorting.RegelFastsettUtbetalingsbeløpTilBruker;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok.DokumentasjonRegelBeregnBruttoPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok.DokumentasjonRegelBeregningsgrunnlagATFL;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok.DokumentasjonRegelFastsettAvkortetBGOver6GNårRefusjonUnder6G;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok.DokumentasjonRegelFastsettAvkortetVedRefusjonOver6G;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok.DokumentasjonRegelFastsetteBeregningsgrunnlagForKombinasjonATFLSN;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok.DokumentasjonRegelForeslåBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.dok.DokumentasjonRegelFullføreBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fastsette.refusjon.over6g.RegelBeregnRefusjonPrArbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.ytelse.dagpengerelleraap.RegelFastsettBeregningsgrunnlagDPellerAAP;
import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;
import no.nav.fpsak.nare.specification.Specification;

public class BeregningsgrunnlagDocTest {
    private Unleash unleash = new FakeUnleash();

    @Test
    public void testKombinasjonATFLSN() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new DokumentasjonRegelFastsetteBeregningsgrunnlagForKombinasjonATFLSN().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }

    @Test
    public void testRegelFastsettBeregningsgrunnlagDPellerAAP() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new RegelFastsettBeregningsgrunnlagDPellerAAP().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }

    @Test
    public void testRegelFastsettAvkortetVedRefusjonOver6G() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new DokumentasjonRegelFastsettAvkortetVedRefusjonOver6G().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }

    @Test
    public void testRegelFastsettAvkortetBGOver6GNårRefusjonUnder6G() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new DokumentasjonRegelFastsettAvkortetBGOver6GNårRefusjonUnder6G().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }

    @Test
    public void testRegelBeregnRefusjonPrArbeidsforhold() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new RegelBeregnRefusjonPrArbeidsforhold().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }

    @Test
    public void testRegelBeregningsgrunnlagATFL() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new DokumentasjonRegelBeregningsgrunnlagATFL().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }

    @Test
    public void testRegelBeregnBruttoPrArbeidsforhold() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new DokumentasjonRegelBeregnBruttoPrArbeidsforhold().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }

    @Test
    public void testRegelFastsettUtbetalingsbeløpTilBruker() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new RegelFastsettUtbetalingsbeløpTilBruker().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }

    @Test
    public void testRegelForeslåBeregningsgrunnlag() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new DokumentasjonRegelForeslåBeregningsgrunnlag(unleash).getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }

    @Test
    public void testRegelFullføreBeregningsgrunnlag() throws Exception {
        Specification<BeregningsgrunnlagPeriode> beregning = new DokumentasjonRegelFullføreBeregningsgrunnlag().getSpecification();
        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }
}
