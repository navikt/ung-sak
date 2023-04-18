package no.nav.k9.sak.ytelse.beregning;

import java.math.RoundingMode;
import java.time.Year;

import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.FeriepengeOppsummering;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.RegelBeregnFeriepenger;

public class FeriepengeBeregner {

    private FeriepengeBeregner(){
    }

    public static void beregnFeriepenger(BeregningsresultatEntitet beregningsresultat, BeregningsresultatFeriepengerRegelModell regelModell) {
        String regelInput = JacksonJsonConfig.toJson(regelModell);

        RegelBeregnFeriepenger regelBeregnFeriepenger = new RegelBeregnFeriepenger();
        Evaluation evaluation = regelBeregnFeriepenger.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        beregningsresultat.setFeriepengerRegelInput(regelInput);
        beregningsresultat.setFeriepengerRegelSporing(sporing);

        MapBeregningsresultatFeriepengerFraRegelTilVL.mapTilResultatFraRegelModell(beregningsresultat, regelModell);
    }

    public static FeriepengeOppsummering beregnFeriepengerOppsummering(BeregningsresultatFeriepengerRegelModell regelModell) {
        RegelBeregnFeriepenger regelBeregnFeriepenger = new RegelBeregnFeriepenger();
        regelBeregnFeriepenger.evaluer(regelModell);

        FeriepengeOppsummering.Builder feriepengeoppsummeringBuilder = new FeriepengeOppsummering.Builder();

        for (var brPeriode : regelModell.getBeregningsresultatPerioder()) {
            for (var brAndel : brPeriode.getBeregningsresultatAndelList()) {
                for (BeregningsresultatFeriepengerPrÅr feriepengerPrÅr : brAndel.getBeregningsresultatFeriepengerPrÅrListe()) {
                    feriepengeoppsummeringBuilder.leggTil(Year.of(feriepengerPrÅr.getOpptjeningÅr().getYear()), brAndel.getMottakerType(), brAndel.getMottakerType() == MottakerType.ARBEIDSGIVER ? brAndel.getArbeidsgiverId() : null, feriepengerPrÅr.getÅrsbeløp().setScale(0, RoundingMode.UNNECESSARY).longValue());
                }
            }
        }
        return feriepengeoppsummeringBuilder.build();
    }
}
