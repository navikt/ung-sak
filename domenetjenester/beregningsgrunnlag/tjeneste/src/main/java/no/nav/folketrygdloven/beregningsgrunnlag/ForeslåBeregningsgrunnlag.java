package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.finn.unleash.Unleash;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.regelmodelltilvl.MapBeregningsgrunnlagFraRegelTilVL;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.input.BeregningsgrunnlagInput;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.KortvarigArbeidsforholdTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagPrStatusOgAndel;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningsgrunnlagRegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.RegelmodellOversetter;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.foreslå.RegelForeslåBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.RegelResultat;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.Beregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.regelmodell.regelmodell.resultat.BeregningsgrunnlagPeriode;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Yrkesaktivitet;
import no.nav.foreldrepenger.domene.iay.modell.YrkesaktivitetFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.fpsak.nare.evaluation.Evaluation;

@ApplicationScoped
public class ForeslåBeregningsgrunnlag {

    private MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel;
    private MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel;
    private AksjonspunktUtlederForeslåBeregning aksjonspunktUtleder;
    private Unleash unleash;

    ForeslåBeregningsgrunnlag() {
        //for CDI proxy
    }

    @Inject
    public ForeslåBeregningsgrunnlag(MapBeregningsgrunnlagFraVLTilRegel oversetterTilRegel,
                                     MapBeregningsgrunnlagFraRegelTilVL oversetterFraRegel,
                                     AksjonspunktUtlederForeslåBeregning aksjonspunktUtleder,
                                     Unleash unleash) {
        this.oversetterTilRegel = oversetterTilRegel;
        this.oversetterFraRegel = oversetterFraRegel;
        this.aksjonspunktUtleder = aksjonspunktUtleder;
        this.unleash = unleash;
    }

    public BeregningsgrunnlagRegelResultat foreslåBeregningsgrunnlag(BeregningsgrunnlagInput input, BeregningsgrunnlagGrunnlagEntitet grunnlag) {
        // Oversetter initielt Beregningsgrunnlag -> regelmodell
        var ref = input.getBehandlingReferanse();
        Beregningsgrunnlag regelmodellBeregningsgrunnlag = oversetterTilRegel.map(input, grunnlag);
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet beregningsgrunnlag = grunnlag.getBeregningsgrunnlag().orElse(null);
        opprettPerioderForKortvarigeArbeidsforhold(ref.getAktørId(), regelmodellBeregningsgrunnlag, beregningsgrunnlag, input.getIayGrunnlag());
        String jsonInput = toJson(regelmodellBeregningsgrunnlag);

        // Evaluerer hver BeregningsgrunnlagPeriode fra initielt Beregningsgrunnlag
        List<RegelResultat> regelResultater = new ArrayList<>();
        for (BeregningsgrunnlagPeriode periode : regelmodellBeregningsgrunnlag.getBeregningsgrunnlagPerioder()) {
            Evaluation evaluation = new RegelForeslåBeregningsgrunnlag(periode, unleash).evaluer(periode);
            regelResultater.add(RegelmodellOversetter.getRegelResultat(evaluation, jsonInput));
        }

        // Oversett endelig resultat av regelmodell til foreslått Beregningsgrunnlag  (+ spore input -> evaluation)
        no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet foreslåttBeregningsgrunnlag =
            oversetterFraRegel.mapForeslåBeregningsgrunnlag(regelmodellBeregningsgrunnlag, regelResultater, beregningsgrunnlag);
        List<BeregningAksjonspunktResultat> aksjonspunkter = aksjonspunktUtleder.utledAksjonspunkter(input, regelResultater);
        BeregningsgrunnlagVerifiserer.verifiserForeslåttBeregningsgrunnlag(foreslåttBeregningsgrunnlag);
        return new BeregningsgrunnlagRegelResultat(foreslåttBeregningsgrunnlag, aksjonspunkter);
    }

    private void opprettPerioderForKortvarigeArbeidsforhold(AktørId aktørId, Beregningsgrunnlag regelBeregningsgrunnlag, BeregningsgrunnlagEntitet vlBeregningsgrunnlag, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        var filter = getYrkesaktivitetFilter(aktørId, iayGrunnlag);
        Map<BeregningsgrunnlagPrStatusOgAndel, Yrkesaktivitet> kortvarigeAktiviteter = KortvarigArbeidsforholdTjeneste.hentAndelerForKortvarigeArbeidsforhold(aktørId, vlBeregningsgrunnlag, iayGrunnlag);
        kortvarigeAktiviteter.entrySet().stream()
            .filter(entry -> entry.getKey().getBgAndelArbeidsforhold()
                .filter(a -> Boolean.TRUE.equals(a.getErTidsbegrensetArbeidsforhold())).isPresent())
            .map(Map.Entry::getValue)
            .forEach(ya -> SplittBGPerioderMedAvsluttetArbeidsforhold.splitt(regelBeregningsgrunnlag, filter.getAnsettelsesPerioder(ya)));
    }

    private YrkesaktivitetFilter getYrkesaktivitetFilter(AktørId aktørId, InntektArbeidYtelseGrunnlag iayGrunnlag) {
        return  new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(), iayGrunnlag.getAktørArbeidFraRegister(aktørId));
    }

    private String toJson(Beregningsgrunnlag beregningsgrunnlagRegel) {
        return JacksonJsonConfig.toJson(beregningsgrunnlagRegel, BeregningsgrunnlagFeil.FEILFACTORY::kanIkkeSerialisereRegelinput);
    }

}
