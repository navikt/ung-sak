package no.nav.foreldrepenger.ytelse.beregning;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.folketrygdloven.beregningsgrunnlag.RegelmodellOversetter;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraRegelTilVL;
import no.nav.foreldrepenger.ytelse.beregning.adapter.MapBeregningsresultatFeriepengerFraVLTilRegel;
import no.nav.foreldrepenger.ytelse.beregning.regelmodell.feriepenger.BeregningsresultatFeriepengerRegelModell;
import no.nav.foreldrepenger.ytelse.beregning.regler.feriepenger.RegelBeregnFeriepenger;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatFeriepenger;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

public abstract class BeregnFeriepengerTjeneste {

    private JacksonJsonConfig jacksonJsonConfig = new JacksonJsonConfig();
    private int antallDagerFeriepenger;

    protected BeregnFeriepengerTjeneste() {
        //NOSONAR
    }

    public BeregnFeriepengerTjeneste(int antallDagerFeriepenger) {
        if (antallDagerFeriepenger == 0) {
            throw new IllegalStateException("Injeksjon av antallDagerFeriepenger feilet. antallDagerFeriepenger kan ikke være 0.");
        }
        this.antallDagerFeriepenger = antallDagerFeriepenger;
    }

    public void beregnFeriepenger(BeregningsresultatEntitet beregningsresultat) {

        BeregningsresultatFeriepengerRegelModell regelModell = MapBeregningsresultatFeriepengerFraVLTilRegel.mapFra(beregningsresultat, antallDagerFeriepenger);
        String regelInput = toJson(regelModell);

        RegelBeregnFeriepenger regelBeregnFeriepenger = new RegelBeregnFeriepenger();
        Evaluation evaluation = regelBeregnFeriepenger.evaluer(regelModell);
        String sporing = RegelmodellOversetter.getSporing(evaluation);

        BeregningsresultatFeriepenger beregningsresultatFeriepenger = BeregningsresultatFeriepenger.builder()
            .medFeriepengerRegelInput(regelInput)
            .medFeriepengerRegelSporing(sporing)
            .build(beregningsresultat);

        MapBeregningsresultatFeriepengerFraRegelTilVL.mapFra(beregningsresultat, regelModell, beregningsresultatFeriepenger);
    }

    private String toJson(BeregningsresultatFeriepengerRegelModell grunnlag) {
        JacksonJsonConfig var10000 = this.jacksonJsonConfig;
        BeregnFeriepengerFeil var10002 = BeregnFeriepengerFeil.FACTORY;
        return var10000.toJson(grunnlag, var10002::jsonMappingFeilet);
    }

    interface BeregnFeriepengerFeil extends DeklarerteFeil {
        BeregnFeriepengerFeil FACTORY = FeilFactory.create(BeregnFeriepengerFeil.class); // NOSONAR ok med konstant

        @TekniskFeil(feilkode = "FP-985762", feilmelding = "JSON mapping feilet", logLevel = LogLevel.ERROR)
        Feil jsonMappingFeilet(JsonProcessingException var1);
    }
}
