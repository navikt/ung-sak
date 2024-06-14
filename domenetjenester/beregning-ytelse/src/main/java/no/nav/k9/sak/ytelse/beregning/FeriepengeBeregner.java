package no.nav.k9.sak.ytelse.beregning;

import java.math.RoundingMode;
import java.time.Year;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.k9.sak.ytelse.beregning.regelmodell.MottakerType;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerPrÅr;
import no.nav.k9.sak.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.FeriepengeOppsummering;
import no.nav.k9.sak.ytelse.beregning.regler.feriepenger.RegelBeregnFeriepenger;

@ApplicationScoped
public class FeriepengeBeregner {

    private boolean skalKjøreDagpengeregel;

    FeriepengeBeregner(){
    }

    @Inject
    public FeriepengeBeregner(@KonfigVerdi(value = "FERIEPENGER_AV_DAGPENGER", defaultVerdi = "false") boolean skalKjøreDagpengeregel) {
        this.skalKjøreDagpengeregel = skalKjøreDagpengeregel;
    }

    public void beregnFeriepenger(BeregningsresultatEntitet beregningsresultat, BeregningsresultatFeriepengerRegelModell regelModell) {
        String regelInput = JacksonJsonConfig.toJson(regelModell);

        RegelBeregnFeriepenger regelBeregnFeriepenger = new RegelBeregnFeriepenger(skalKjøreDagpengeregel);
        Evaluation evaluation = regelBeregnFeriepenger.evaluer(regelModell);
        String sporing = EvaluationSerializer.asJson(evaluation);

        beregningsresultat.setFeriepengerRegelInput(regelInput);
        beregningsresultat.setFeriepengerRegelSporing(sporing);

        MapBeregningsresultatFeriepengerFraRegelTilVL.mapTilResultatFraRegelModell(beregningsresultat, regelModell);
    }

    public FeriepengeOppsummering beregnFeriepengerOppsummering(BeregningsresultatFeriepengerRegelModell regelModell) {
        RegelBeregnFeriepenger regelBeregnFeriepenger = new RegelBeregnFeriepenger(skalKjøreDagpengeregel);
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
