package no.nav.k9.sak.ytelse.pleiepengerbarn.inngangsvilkår.omsorgenfor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.BostedsAdresse;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.DefaultOmsorgenForVilkår;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.Relasjon;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.RelasjonsRolle;

public class OmsorgenForVilkårTest {

    @Test
    public void skal_få_avslag_hvis_omsorgsperson_bor_sammen_med() {
        final var grunnlag = new OmsorgenForVilkårGrunnlag(null,
            List.of(new BostedsAdresse("1", "a", null, null, "1234", "NOR")),
            List.of(new BostedsAdresse("2", "a", null, null, "9999", "NOR")), null);

        final var evaluation = new DefaultOmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.NEI);
    }

    @Test
    public void skal_få_avslag_hvis_omsorgsperson_bor_sammen_med_og_saksbehandler_ikke_har_vurdert() {
        final var grunnlag = new OmsorgenForVilkårGrunnlag(null,
            List.of(new BostedsAdresse("1", "a", null, null, "1234", "NOR")),
            List.of(new BostedsAdresse("2", "a", null, null, "1234", "NOR")), null);

        final var evaluation = new DefaultOmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.NEI);
    }

    @Test
    public void skal_IKKE_få_avslag_hvis_mor_far_sammen() {
        final var grunnlag = new OmsorgenForVilkårGrunnlag(new Relasjon("1", "2", RelasjonsRolle.BARN, true), List.of(), List.of(), null);

        final var evaluation = new DefaultOmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_IKKE_få_avslag_hvis_mor_bor_sammen() {
        final var grunnlag = new OmsorgenForVilkårGrunnlag(new Relasjon("1", "2", RelasjonsRolle.BARN, true), List.of(), List.of(), null);

        final var evaluation = new DefaultOmsorgenForVilkår().evaluer(grunnlag);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_IKKE_få_avslag_hvis_far_ikke_bor_sammen_men_saksbehandler_mener_det_er_omsorg() {
        final var grunnlag = new OmsorgenForVilkårGrunnlag(new Relasjon("1", "2", RelasjonsRolle.BARN, false), List.of(), List.of(), true);

        final var evaluation = new DefaultOmsorgenForVilkår().evaluer(grunnlag);
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
