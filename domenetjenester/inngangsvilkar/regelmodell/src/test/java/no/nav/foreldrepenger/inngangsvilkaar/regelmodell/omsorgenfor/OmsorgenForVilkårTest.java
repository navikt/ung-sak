package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.omsorgenfor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;

public class OmsorgenForVilkårTest {

    @Test
    public void skal_få_avslag_hvis_omsorgsperson_bor_sammen_med() {
        final var grunnlag = new OmsorgenForGrunnlag(null,
            List.of(new BostedsAdresse("1", "a", null, null, "1234", "NOR")),
            List.of(new BostedsAdresse("2", "a", null, null, "9999", "NOR")));

        final var evaluation = new OmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.NEI);
    }

    @Test
    public void skal_IKKE_få_avslag_hvis_omsorgsperson_bor_sammen_med() {
        final var grunnlag = new OmsorgenForGrunnlag(null,
            List.of(new BostedsAdresse("1", "a", null, null, "1234", "NOR")),
            List.of(new BostedsAdresse("2", "a", null, null, "1234", "NOR")));

        final var evaluation = new OmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_IKKE_få_avslag_hvis_mor_far_sammen() {
        final var grunnlag = new OmsorgenForGrunnlag(new Relasjon("1", "2", RelasjonsRolle.FARA, true), List.of(), List.of());

        final var evaluation = new OmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_IKKE_få_avslag_hvis_mor_bor_sammen() {
        final var grunnlag = new OmsorgenForGrunnlag(new Relasjon("1", "2", RelasjonsRolle.MORA, true), List.of(), List.of());

        final var evaluation = new OmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_IKKE_få_avslag_hvis_far_ikke_bor_sammen() {
        final var grunnlag = new OmsorgenForGrunnlag(new Relasjon("1", "2", RelasjonsRolle.FARA, false), List.of(), List.of());

        final var evaluation = new OmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_IKKE_få_avslag_hvis_mor_ikke_bor_sammen() {
        final var grunnlag = new OmsorgenForGrunnlag(new Relasjon("1", "2", RelasjonsRolle.MORA, false), List.of(), List.of());

        final var evaluation = new OmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    private Resultat getUtfall(EvaluationSummary summary) {
        Collection<Evaluation> leafEvaluations = summary.leafEvaluations();
        for (Evaluation ev : leafEvaluations) {
            if (ev.getOutcome() != null) {
                Resultat res = ev.result();
                switch (res) {
                    case JA:
                        return Resultat.JA;
                    case NEI:
                        return Resultat.NEI;
                    case IKKE_VURDERT:
                        return Resultat.IKKE_VURDERT;
                    default:
                        throw new IllegalArgumentException("Ukjent Resultat:" + res + " ved evaluering av:" + ev);
                }
            } else {
                return Resultat.JA;
            }
        }

        throw new IllegalArgumentException("leafEvaluations.isEmpty():" + leafEvaluations);
    }
}
