package no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.omsorgenfor;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.BostedsAdresse;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.Fosterbarn;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForKnekkpunkter;
import no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell.OmsorgenForVilkårGrunnlag;
import no.nav.k9.sak.ytelse.omsorgspenger.inngangsvilkår.omsorgenfor.regelmodell.OMPOmsorgenForVilkår;

public class OmsorgenForVilkårTest {

    private DatoIntervallEntitet periodeTilVurdering = DatoIntervallEntitet.fraOgMedTilOgMed(LocalDate.now().minusWeeks(4), LocalDate.now());;

    @Test
    public void skal_få_innvilget_når_søker_og_barn_har_samme_bosted() {

        final var grunnlag = new OmsorgenForVilkårGrunnlag(null,
            List.of(new BostedsAdresse("1", "a", null, null, "1234", "NOR")),
            List.of(new BostedsAdresse("2", "a", null, null, "1234", "NOR")),
            null,
            List.of(),
            List.of());

        final var evaluation = new OMPOmsorgenForVilkår().evaluer(grunnlag, new OmsorgenForKnekkpunkter(periodeTilVurdering));
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_ikke_få_innvilget_når_søker_og_barn_ikke_har_samme_bosted() {

        final var grunnlag = new OmsorgenForVilkårGrunnlag(null,
            List.of(new BostedsAdresse("1", "a", null, null, "1234", "NOR")),
            List.of(new BostedsAdresse("2", "b", null, null, "5678", "NOR")),
            null,
            List.of(),
            List.of());

        final var evaluation = new OMPOmsorgenForVilkår().evaluer(grunnlag, new OmsorgenForKnekkpunkter(periodeTilVurdering));
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.NEI);
    }

    @Test
    public void skal_få_innvilget_når_søker_og_barn_har_delt_bosted() {

        final var grunnlag = new OmsorgenForVilkårGrunnlag(null,
            List.of(new BostedsAdresse("1", "a", null, null, "1234", "NOR")),
            List.of(new BostedsAdresse("2", "b", null, null, "5678", "NOR")),
            null,
            List.of(),
            List.of(new BostedsAdresse("2", "a", null, null, "1234", "NOR")));

        final var evaluation = new OMPOmsorgenForVilkår().evaluer(grunnlag, new OmsorgenForKnekkpunkter(periodeTilVurdering));
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_få_innvilget_når_søker_har_fosterbarn() {

        final var grunnlag = new OmsorgenForVilkårGrunnlag(null,
            List.of(new BostedsAdresse("1", "a", null, null, "1234", "NOR")),
            List.of(new BostedsAdresse("2", "b", null, null, "5678", "NOR")),
            null,
            List.of(new Fosterbarn("3", LocalDate.now().withDayOfMonth(1).withMonth(1), null)),
            List.of());

        final var evaluation = new OMPOmsorgenForVilkår().evaluer(grunnlag, new OmsorgenForKnekkpunkter(periodeTilVurdering));
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);
    }

    @Test
    public void skal_få_innvilget_når_søker_ikke_bor_med_noen_barn_fordi_saksbehandler_har_vurdert_at_det_er_omsorg() {

        final var grunnlag = new OmsorgenForVilkårGrunnlag(null,
            List.of(new BostedsAdresse("1", "a", null, null, "1234", "NOR")),
            List.of(new BostedsAdresse("2", "b", null, null, "5678", "NOR")),
            true,
            List.of(),
            List.of());

        final var evaluation = new OMPOmsorgenForVilkår().evaluer(grunnlag, new OmsorgenForKnekkpunkter(periodeTilVurdering));
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
