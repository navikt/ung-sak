package no.nav.folketrygdloven.beregningsgrunnlag.verdikjede;

import no.nav.folketrygdloven.beregningsgrunnlag.BeregningsgrunnlagAksjonspunktUtleder;
import no.nav.folketrygdloven.beregningsgrunnlag.FastsettBeregningAktiviteter;
import no.nav.folketrygdloven.beregningsgrunnlag.FastsettBeregningsgrunnlagPerioderTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.FastsettSkjæringstidspunktOgStatuser;
import no.nav.folketrygdloven.beregningsgrunnlag.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.ForeslåBeregningsgrunnlag;
import no.nav.folketrygdloven.beregningsgrunnlag.VurderBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.adapter.vltilregelmodell.MapBeregningsgrunnlagFraVLTilRegel;
import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.BeregningIAYTestUtil;
import no.nav.folketrygdloven.beregningsgrunnlag.ytelse.fp.FullføreBeregningsgrunnlagFPImpl;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektsmeldingTjeneste;

class BeregningTjenesteWrapper {

    private FastsettBeregningAktiviteter fastsettBeregningAktiviteter = new FastsettBeregningAktiviteter();

    private ForeslåBeregningsgrunnlag foreslåBeregningsgrunnlagTjeneste;
    private FullføreBeregningsgrunnlagFPImpl fullføreBeregningsgrunnlagTjeneste;
    private FordelBeregningsgrunnlagTjeneste fordelBeregningsgrunnlagTjeneste;
    private BeregningsgrunnlagAksjonspunktUtleder aksjonspunktUtlederFaktaOmBeregning;
    private FastsettBeregningsgrunnlagPerioderTjeneste fastsettBeregningsgrunnlagPerioderTjeneste;
    private FastsettSkjæringstidspunktOgStatuser fastsettSkjæringstidspunktOgStatuser;
    private BeregningIAYTestUtil beregningIAYTestUtil;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private InntektsmeldingTjeneste inntektsmeldingTjeneste;
    private MapBeregningsgrunnlagFraVLTilRegel mapBeregningsgrunnlagFraVLTilRegel;
    private VurderBeregningsgrunnlagTjeneste vurderBeregningsgrunnlagTjeneste;

    public BeregningTjenesteWrapper(ForeslåBeregningsgrunnlag foreslåBeregningsgrunnlagTjeneste,
                                    FullføreBeregningsgrunnlagFPImpl fullføreBeregningsgrunnlagTjeneste,
                                    FordelBeregningsgrunnlagTjeneste fordelBeregningsgrunnlagTjeneste,
                                    BeregningsgrunnlagAksjonspunktUtleder aksjonspunktUtlederFaktaOmBeregning,
                                    FastsettBeregningsgrunnlagPerioderTjeneste fastsettBeregningsgrunnlagPerioderTjeneste,
                                    FastsettSkjæringstidspunktOgStatuser fastsettSkjæringstidspunktOgStatuser,
                                    BeregningIAYTestUtil beregningIAYTestUtil,
                                    InntektArbeidYtelseTjeneste iayTjeneste,
                                    InntektsmeldingTjeneste inntektsmeldingTjeneste,
                                    MapBeregningsgrunnlagFraVLTilRegel mapBeregningsgrunnlagFraVLTilRegel,
                                    VurderBeregningsgrunnlagTjeneste vurderBeregningsgrunnlagTjeneste) {
        this.foreslåBeregningsgrunnlagTjeneste = foreslåBeregningsgrunnlagTjeneste;
        this.fullføreBeregningsgrunnlagTjeneste = fullføreBeregningsgrunnlagTjeneste;
        this.fordelBeregningsgrunnlagTjeneste = fordelBeregningsgrunnlagTjeneste;
        this.aksjonspunktUtlederFaktaOmBeregning = aksjonspunktUtlederFaktaOmBeregning;
        this.fastsettBeregningsgrunnlagPerioderTjeneste = fastsettBeregningsgrunnlagPerioderTjeneste;
        this.fastsettSkjæringstidspunktOgStatuser = fastsettSkjæringstidspunktOgStatuser;
        this.beregningIAYTestUtil = beregningIAYTestUtil;
        this.iayTjeneste = iayTjeneste;
        this.inntektsmeldingTjeneste = inntektsmeldingTjeneste;
        this.mapBeregningsgrunnlagFraVLTilRegel = mapBeregningsgrunnlagFraVLTilRegel;
        this.vurderBeregningsgrunnlagTjeneste = vurderBeregningsgrunnlagTjeneste;
    }

    public ForeslåBeregningsgrunnlag getForeslåBeregningsgrunnlagTjeneste() {
        return foreslåBeregningsgrunnlagTjeneste;
    }

    public FullføreBeregningsgrunnlagFPImpl getFullføreBeregningsgrunnlagTjeneste() {
        return fullføreBeregningsgrunnlagTjeneste;
    }

    public FordelBeregningsgrunnlagTjeneste getFordelBeregningsgrunnlagTjeneste() {
        return fordelBeregningsgrunnlagTjeneste;
    }

    public BeregningsgrunnlagAksjonspunktUtleder getAksjonspunktUtlederFaktaOmBeregning() {
        return aksjonspunktUtlederFaktaOmBeregning;
    }

    public FastsettBeregningsgrunnlagPerioderTjeneste getFastsettBeregningsgrunnlagPerioderTjeneste() {
        return fastsettBeregningsgrunnlagPerioderTjeneste;
    }

    public FastsettBeregningAktiviteter getFastsettBeregningAktiviteter() {
        return fastsettBeregningAktiviteter;
    }

    public FastsettSkjæringstidspunktOgStatuser getFastsettSkjæringstidspunktOgStatuser() {
        return fastsettSkjæringstidspunktOgStatuser;
    }

    public BeregningIAYTestUtil getBeregningIAYTestUtil() {
        return beregningIAYTestUtil;
    }

    public InntektArbeidYtelseTjeneste getIayTjeneste() {
        return iayTjeneste;
    }

    public InntektsmeldingTjeneste getInntektsmeldingTjeneste() {
        return inntektsmeldingTjeneste;
    }

    public MapBeregningsgrunnlagFraVLTilRegel getMapBeregningsgrunnlagFraVLTilRegel() {
        return mapBeregningsgrunnlagFraVLTilRegel;
    }

    public VurderBeregningsgrunnlagTjeneste getVurderBeregningsgrunnlagTjeneste() {
        return vurderBeregningsgrunnlagTjeneste;
    }
}
