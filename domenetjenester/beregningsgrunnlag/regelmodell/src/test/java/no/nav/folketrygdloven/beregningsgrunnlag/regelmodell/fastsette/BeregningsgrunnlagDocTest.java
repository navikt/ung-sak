package no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.fastsette;

import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.GRUNNBELØP_2017;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.settoppGrunnlagMedEnPeriode;
import static no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.BeregningsgrunnlagScenario.settoppMånedsinntekter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import no.finn.unleash.FakeUnleash;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.foreslå.RegelForeslåBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.AktivitetStatus;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Arbeidsforhold;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.grunnlag.inntekt.Inntektskilde;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.fpsak.nare.doc.RuleDescriptionDigraph;
import no.nav.fpsak.nare.specification.Specification;

public class BeregningsgrunnlagDocTest {
    private FakeUnleash unleash = new FakeUnleash();

    @Test
    public void test_documentation() throws Exception {
        String orgnr = "987654321";
        LocalDate skjæringstidspunkt = LocalDate.of(2018, Month.JANUARY, 15);
        BigDecimal månedsinntekt = BigDecimal.valueOf(GRUNNBELØP_2017 / 12 / 2);
        Arbeidsforhold arbeidsforhold = Arbeidsforhold.nyttArbeidsforholdHosVirksomhet(orgnr);
        Inntektsgrunnlag inntektsgrunnlag = settoppMånedsinntekter(skjæringstidspunkt,
                List.of(månedsinntekt, månedsinntekt, månedsinntekt), Inntektskilde.INNTEKTSKOMPONENTEN_BEREGNING, arbeidsforhold);
        BeregningsgrunnlagPeriode grunnlag = settoppGrunnlagMedEnPeriode(skjæringstidspunkt, inntektsgrunnlag, List.of(AktivitetStatus.ATFL),
                List.of(arbeidsforhold), Collections.singletonList(månedsinntekt.multiply(BigDecimal.valueOf(12)))).getBeregningsgrunnlagPerioder().get(0);

        Specification<BeregningsgrunnlagPeriode> beregning = new RegelForeslåBeregningsgrunnlag(grunnlag, unleash).getSpecification();

        RuleDescriptionDigraph digraph = new RuleDescriptionDigraph(beregning.ruleDescription());

        @SuppressWarnings("unused")
        String json = digraph.toJson();

//        System.out.println(json);
    }
}
