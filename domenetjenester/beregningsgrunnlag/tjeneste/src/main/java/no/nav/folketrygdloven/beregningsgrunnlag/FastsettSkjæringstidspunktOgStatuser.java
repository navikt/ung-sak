package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBGSkjæringstidspunktOgStatuserFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBGStatuserFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningAktivitetAggregatEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelmodellOversetter;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.regel.RegelFastsettSkjæringstidspunkt;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.regelmodell.AktivitetStatusModell;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.skjæringstidspunkt.status.RegelFastsettStatusVedSkjæringstidspunkt;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.fpsak.nare.evaluation.Evaluation;

@ApplicationScoped
public class FastsettSkjæringstidspunktOgStatuser {

    private MapBGSkjæringstidspunktOgStatuserFraRegelTilVL oversetterFraRegel;
    private MapBGStatuserFraVLTilRegel oversetterTilRegel;

    FastsettSkjæringstidspunktOgStatuser() {
        // for CDI proxy
    }

    @Inject
    public FastsettSkjæringstidspunktOgStatuser(MapBGSkjæringstidspunktOgStatuserFraRegelTilVL oversetterFraRegel,
                                                MapBGStatuserFraVLTilRegel oversetterTilRegel) {
        this.oversetterFraRegel = oversetterFraRegel;
        this.oversetterTilRegel = oversetterTilRegel;
    }

    public BeregningsgrunnlagEntitet fastsett(BehandlingReferanse ref, BeregningAktivitetAggregatEntitet beregningAktivitetAggregat, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        AktivitetStatusModell regelmodell = oversetterTilRegel.map(beregningAktivitetAggregat);

        // Tar sporingssnapshot av regelmodell, deretter oppdateres modell med fastsatt skjæringstidspunkt for Beregning
        String inputSkjæringstidspunkt = toJson(regelmodell);
        Evaluation evaluationSkjæringstidspunkt = new RegelFastsettSkjæringstidspunkt().evaluer(regelmodell);

        // Tar sporingssnapshot av regelmodell, deretter oppdateres modell med status per beregningsgrunnlag
        String inputStatusFastsetting = toJson(regelmodell);
        Evaluation evaluationStatusFastsetting = new RegelFastsettStatusVedSkjæringstidspunkt().evaluer(regelmodell);

        // Oversett endelig resultat av regelmodell (+ spore input -> evaluation)
        List<RegelResultat> regelResultater = List.of(
            RegelmodellOversetter.getRegelResultat(evaluationSkjæringstidspunkt, inputSkjæringstidspunkt),
            RegelmodellOversetter.getRegelResultat(evaluationStatusFastsetting, inputStatusFastsetting));
        return oversetterFraRegel.mapForSkjæringstidspunktOgStatuser(ref, regelmodell, regelResultater, iayGrunnlag);
    }

    private String toJson(AktivitetStatusModell grunnlag) {
        return JacksonJsonConfig.toJson(grunnlag, BeregningsgrunnlagFeil.FEILFACTORY::kanIkkeSerialisereRegelinput);
    }
}
