package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.periodisering.MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.perioder.FastsettPeriodeRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.PeriodeModell;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.SplittetPeriode;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
public class FastsettBeregningsgrunnlagPerioderTjeneste {
    public static final int MÅNEDER_I_1_ÅR = 12;

    private MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse oversetterTilRegelNaturalytelse;
    private Instance<MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering> oversetterTilRegelRefusjonOgGradering;
    private MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse oversetterFraRegelNaturalytelse;
    private MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering oversetterFraRegelRefusjonsOgGradering;

    FastsettBeregningsgrunnlagPerioderTjeneste() {
        // For CDI
    }

    @Inject
    public FastsettBeregningsgrunnlagPerioderTjeneste(MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelNaturalYtelse oversetterTilRegelNaturalytelse,
                                                          @Any Instance<MapFastsettBeregningsgrunnlagPerioderFraVLTilRegelRefusjonOgGradering> oversetterTilRegelRefusjonOgGradering,
                                                          MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLNaturalytelse oversetterFraRegelNaturalytelse,
                                                          MapFastsettBeregningsgrunnlagPerioderFraRegelTilVLRefusjonOgGradering oversetterFraRegelRefusjonsOgGradering) {
        this.oversetterTilRegelNaturalytelse = oversetterTilRegelNaturalytelse;
        this.oversetterTilRegelRefusjonOgGradering = oversetterTilRegelRefusjonOgGradering;
        this.oversetterFraRegelNaturalytelse = oversetterFraRegelNaturalytelse;
        this.oversetterFraRegelRefusjonsOgGradering = oversetterFraRegelRefusjonsOgGradering;
    }

    
    public BeregningsgrunnlagEntitet fastsettPerioderForNaturalytelse(BeregningsgrunnlagInput input,
                                                               BeregningsgrunnlagEntitet beregningsgrunnlag) {
        PeriodeModell periodeModell = oversetterTilRegelNaturalytelse.map(input, beregningsgrunnlag);
        return kjørRegelOgMapTilVLNaturalytelse(beregningsgrunnlag, periodeModell);
    }

    
    public BeregningsgrunnlagEntitet fastsettPerioderForRefusjonOgGradering(BeregningsgrunnlagInput input,
                                                                     BeregningsgrunnlagEntitet beregningsgrunnlag) {
        var ref = input.getBehandlingReferanse();
        var mapper = FagsakYtelseTypeRef.Lookup.find(oversetterTilRegelRefusjonOgGradering, ref.getFagsakYtelseType())
            .orElseThrow(() -> new IllegalStateException("Finner ikke implementasjon for håndtering av refusjon/gradering for BehandlingReferanse " + ref));
        
        PeriodeModell periodeModell = mapper.map(input, beregningsgrunnlag);
        return kjørRegelOgMapTilVLRefusjonOgGradering(beregningsgrunnlag, periodeModell);
    }

    private BeregningsgrunnlagEntitet kjørRegelOgMapTilVLNaturalytelse(BeregningsgrunnlagEntitet beregningsgrunnlag, PeriodeModell input) {
        String regelInput = toJson(input);
        List<SplittetPeriode> splittedePerioder = FastsettPeriodeRegel.fastsett(input);
        return oversetterFraRegelNaturalytelse.mapFraRegel(splittedePerioder, regelInput, beregningsgrunnlag);
    }

    private BeregningsgrunnlagEntitet kjørRegelOgMapTilVLRefusjonOgGradering(BeregningsgrunnlagEntitet beregningsgrunnlag, PeriodeModell input) {
        String regelInput = toJson(input);
        List<SplittetPeriode> splittedePerioder = FastsettPeriodeRegel.fastsett(input);
        return oversetterFraRegelRefusjonsOgGradering.mapFraRegel(splittedePerioder, regelInput, beregningsgrunnlag);
    }

    private String toJson(Object o) {
        return JacksonJsonConfig.toJson(o, BeregningsgrunnlagFeil.FEILFACTORY::kanIkkeSerialisereRegelinput);
    }
}
