package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.produksjonsstyring.totrinn.BeregningsgrunnlagToTrinn;
import no.nav.k9.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;

@ApplicationScoped
public class OpprettToTrinnsgrunnlag {

    private BeregningTjeneste beregningsgrunnlagTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;

    OpprettToTrinnsgrunnlag() {
        // CDI
    }

    @Inject
    public OpprettToTrinnsgrunnlag(BeregningTjeneste beregningsgrunnlagTjeneste,
                                   TotrinnTjeneste totrinnTjeneste,
                                   BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                   InntektArbeidYtelseTjeneste iayTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    public void settNyttTotrinnsgrunnlag(Behandling behandling) {
        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = iayTjeneste.finnGrunnlag(behandling.getId());

        Totrinnresultatgrunnlag totrinnsresultatgrunnlag = new Totrinnresultatgrunnlag(behandling,
            opprettBeregningToTrinn(BehandlingReferanse.fra(behandling)),
            iayGrunnlagOpt.map(InntektArbeidYtelseGrunnlag::getEksternReferanse).orElse(null));

        totrinnTjeneste.lagreNyttTotrinnresultat(behandling, totrinnsresultatgrunnlag);
    }

    private List<BeregningsgrunnlagToTrinn> opprettBeregningToTrinn(BehandlingReferanse referanse) {
        return beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(referanse, false)
            .stream()
            .map(it -> beregningsgrunnlagTjeneste.hentGrunnlag(referanse, it.getFomDato())
                .map(at -> new BeregningsgrunnlagToTrinn(it.getFomDato()))
                .orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

}
