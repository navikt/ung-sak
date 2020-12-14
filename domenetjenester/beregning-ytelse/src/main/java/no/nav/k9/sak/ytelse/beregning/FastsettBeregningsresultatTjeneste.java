package no.nav.k9.sak.ytelse.beregning;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;

import no.nav.folketrygdloven.beregningsgrunnlag.modell.Beregningsgrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;
import no.nav.fpsak.nare.evaluation.summary.EvaluationSerializer;
import no.nav.k9.sak.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFraRegelTilVL;
import no.nav.k9.sak.ytelse.beregning.adapter.MapBeregningsresultatFraVLTilRegel;
import no.nav.k9.sak.ytelse.beregning.regelmodell.BeregningsresultatRegelmodell;
import no.nav.k9.sak.ytelse.beregning.regelmodell.UttakResultat;
import no.nav.k9.sak.ytelse.beregning.regler.RegelFastsettBeregningsresultat;
import no.nav.vedtak.feil.Feil;
import no.nav.vedtak.feil.FeilFactory;
import no.nav.vedtak.feil.LogLevel;
import no.nav.vedtak.feil.deklarasjon.DeklarerteFeil;
import no.nav.vedtak.feil.deklarasjon.TekniskFeil;

@ApplicationScoped
public class FastsettBeregningsresultatTjeneste {

    private JacksonJsonConfig jacksonJsonConfig = new JacksonJsonConfig();
    private MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel;
    private MapBeregningsresultatFraRegelTilVL mapBeregningsresultatFraRegelTilVL;

    FastsettBeregningsresultatTjeneste() {
        //NOSONAR
    }

    @Inject
    public FastsettBeregningsresultatTjeneste(MapBeregningsresultatFraVLTilRegel mapBeregningsresultatFraVLTilRegel,
                                              MapBeregningsresultatFraRegelTilVL mapBeregningsresultatFraRegelTilVL) {
        this.mapBeregningsresultatFraVLTilRegel = mapBeregningsresultatFraVLTilRegel;
        this.mapBeregningsresultatFraRegelTilVL = mapBeregningsresultatFraRegelTilVL;
    }

    public BeregningsresultatEntitet fastsettBeregningsresultat(List<Beregningsgrunnlag> beregningsgrunnlag, UttakResultat input) {
        // Map til regelmodell
        var regelmodell = mapBeregningsresultatFraVLTilRegel.mapFra(beregningsgrunnlag, input);
        // Kalle regel
        var regel = new RegelFastsettBeregningsresultat();
        var outputContainer = no.nav.k9.sak.ytelse.beregning.regelmodell.Beregningsresultat.builder().build();
        Evaluation evaluation = regel.evaluer(regelmodell, outputContainer);
        String sporing = EvaluationSerializer.asJson(evaluation);

        // Map tilbake til domenemodell fra regelmodell
        var beregningsresultat = BeregningsresultatEntitet.builder()
            .medRegelInput(toJson(regelmodell))
            .medRegelSporing(sporing)
            .build();

        mapBeregningsresultatFraRegelTilVL.mapFra(outputContainer, beregningsresultat);

        return beregningsresultat;
    }

    private String toJson(BeregningsresultatRegelmodell grunnlag) {
        var jsonFac = this.jacksonJsonConfig;
        return jsonFac.toJson(grunnlag, FastsettBeregningsresultatFeil.FACTORY::jsonMappingFeilet);
    }

    interface FastsettBeregningsresultatFeil extends DeklarerteFeil {
        FastsettBeregningsresultatFeil FACTORY = FeilFactory.create(FastsettBeregningsresultatFeil.class); // NOSONAR ok med konstant

        @TekniskFeil(feilkode = "FP-563791",
            feilmelding = "JSON mapping feilet",
            logLevel = LogLevel.ERROR)
        Feil jsonMappingFeilet(JsonProcessingException var1);
    }
}
