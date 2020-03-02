package no.nav.foreldrepenger.inngangsvilkaar.regelmodell.medisinsk;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.Resultat;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSummary;

public class MedisinskvilkårTest {

    @Test
    public void skal_avslå_hvis_det_ikke_er_diagnose() {
        final var grunnlag = new MedisinskvilkårGrunnlag(LocalDate.now().minusWeeks(8), LocalDate.now());
        final var resultat = new MedisinskVilkårResultat();

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, resultat);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.NEI);
    }

    @Test
    public void skal_avslå_hvis_det_ikke_er_dokumentert_av_rett_organ() {
        final var grunnlag = new MedisinskvilkårGrunnlag(LocalDate.now().minusWeeks(8), LocalDate.now())
            .medDiagnoseKode("SYK");
        final var resultat = new MedisinskVilkårResultat();

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, resultat);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.NEI);

        final var grunnlag1 = new MedisinskvilkårGrunnlag(LocalDate.now().minusWeeks(8), LocalDate.now())
            .medDiagnoseKode("SYK")
            .medDiagnoseKilde(DiagnoseKilde.FASTLEGE);
        final var resultat1 = new MedisinskVilkårResultat();

        final var evaluation1 = new Medisinskvilkår().evaluer(grunnlag1, resultat1);
        EvaluationSummary summary1 = new EvaluationSummary(evaluation1);

        assertThat(summary1).isNotNull();
        final var utfall1 = getUtfall(summary1);
        assertThat(utfall1).isNotNull();
        assertThat(utfall1).isEqualTo(Resultat.NEI);
    }

    @Test
    public void skal_avslå_hvis_det_ikke_er_noen_gyldige_perioder() {
        final var grunnlag = new MedisinskvilkårGrunnlag(LocalDate.now().minusWeeks(8), LocalDate.now())
            .medDiagnoseKode("SYK")
            .medDiagnoseKilde(DiagnoseKilde.SYKHUSLEGE);
        final var resultat = new MedisinskVilkårResultat();

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, resultat);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.NEI);

        assertThat(resultat.getPleieperioder()).hasSize(1);
        assertThat(resultat.getPleieperioder().get(0).getGrad()).isEqualTo(Pleiegrad.INGEN);
    }

    @Test
    public void skal_godkjenne_perioden_med_innleggelse() {
        final var tom = LocalDate.now();
        final var fom = tom.minusWeeks(8);
        final var grunnlag = new MedisinskvilkårGrunnlag(fom, tom)
            .medDiagnoseKode("SYK")
            .medDiagnoseKilde(DiagnoseKilde.SYKHUSLEGE)
            .medInnleggelsesPerioder(List.of(new InnleggelsesPeriode(fom, tom)));
        final var resultat = new MedisinskVilkårResultat();

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, resultat);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);

        assertThat(resultat.getPleieperioder()).hasSize(1);
        assertThat(resultat.getPleieperioder().get(0).getGrad()).isEqualTo(Pleiegrad.INNLEGGELSE);
        assertThat(resultat.getPleieperioder().get(0).getFraOgMed()).isEqualTo(fom);
        assertThat(resultat.getPleieperioder().get(0).getTilOgMed()).isEqualTo(tom);
    }

    @Test
    public void skal_prioritere_perioden_med_innleggelse_over_tilsyn() {
        final var tom = LocalDate.now();
        final var fom = tom.minusWeeks(8);
        final var grunnlag = new MedisinskvilkårGrunnlag(fom, tom)
            .medDiagnoseKode("SYK")
            .medDiagnoseKilde(DiagnoseKilde.SYKHUSLEGE)
            .medInnleggelsesPerioder(List.of(new InnleggelsesPeriode(fom, fom.plusWeeks(2))))
            .medKontinuerligTilsyn(List.of(new PeriodeMedKontinuerligTilsyn(fom, tom)));
        final var resultat = new MedisinskVilkårResultat();

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, resultat);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);

        assertThat(resultat.getPleieperioder()).hasSize(2);
        assertThat(resultat.getPleieperioder().get(0).getGrad()).isEqualTo(Pleiegrad.INNLEGGELSE);
        assertThat(resultat.getPleieperioder().get(0).getFraOgMed()).isEqualTo(fom);
        assertThat(resultat.getPleieperioder().get(0).getTilOgMed()).isEqualTo(fom.plusWeeks(2));
        assertThat(resultat.getPleieperioder().get(1).getGrad()).isEqualTo(Pleiegrad.KONTINUERLIG_TILSYN);
        assertThat(resultat.getPleieperioder().get(1).getFraOgMed()).isEqualTo(fom.plusWeeks(2).plusDays(1));
        assertThat(resultat.getPleieperioder().get(1).getTilOgMed()).isEqualTo(tom);
    }

    @Test
    public void skal_prioritere_innleggelse_og_utvidetrett_over_tilsyn() {
        final var tom = LocalDate.now();
        final var fom = tom.minusWeeks(8);
        final var grunnlag = new MedisinskvilkårGrunnlag(fom, tom)
            .medDiagnoseKode("syk")
            .medDiagnoseKilde(DiagnoseKilde.SYKHUSLEGE)
            .medInnleggelsesPerioder(List.of(new InnleggelsesPeriode(fom, fom.plusWeeks(2))))
            .medUtvidetBehov(List.of(new PeriodeMedUtvidetBehov(fom, tom.minusWeeks(2))))
            .medKontinuerligTilsyn(List.of(new PeriodeMedKontinuerligTilsyn(fom, tom)));
        final var resultat = new MedisinskVilkårResultat();

        final var evaluation = new Medisinskvilkår().evaluer(grunnlag, resultat);
        EvaluationSummary summary = new EvaluationSummary(evaluation);

        assertThat(summary).isNotNull();
        final var utfall = getUtfall(summary);
        assertThat(utfall).isNotNull();
        assertThat(utfall).isEqualTo(Resultat.JA);

        assertThat(resultat.getPleieperioder()).hasSize(3);
        assertThat(resultat.getPleieperioder().get(0).getGrad()).isEqualTo(Pleiegrad.INNLEGGELSE);
        assertThat(resultat.getPleieperioder().get(0).getFraOgMed()).isEqualTo(fom);
        assertThat(resultat.getPleieperioder().get(0).getTilOgMed()).isEqualTo(fom.plusWeeks(2));
        assertThat(resultat.getPleieperioder().get(1).getGrad()).isEqualTo(Pleiegrad.UTVIDET_KONTINUERLIG_TILSYN);
        assertThat(resultat.getPleieperioder().get(1).getFraOgMed()).isEqualTo(fom.plusWeeks(2).plusDays(1));
        assertThat(resultat.getPleieperioder().get(1).getTilOgMed()).isEqualTo(tom.minusWeeks(2));
        assertThat(resultat.getPleieperioder().get(2).getGrad()).isEqualTo(Pleiegrad.KONTINUERLIG_TILSYN);
        assertThat(resultat.getPleieperioder().get(2).getFraOgMed()).isEqualTo(tom.minusWeeks(2).plusDays(1));
        assertThat(resultat.getPleieperioder().get(2).getTilOgMed()).isEqualTo(tom);
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
