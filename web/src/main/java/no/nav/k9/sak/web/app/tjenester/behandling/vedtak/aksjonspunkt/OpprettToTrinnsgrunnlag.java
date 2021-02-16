package no.nav.k9.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.kalkulus.BeregningTjeneste;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagVilkårTjeneste;
import no.nav.k9.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.k9.sak.domene.registerinnhenting.InformasjonselementerUtleder;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;
import no.nav.k9.sak.produksjonsstyring.totrinn.BeregningsgrunnlagToTrinn;
import no.nav.k9.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.k9.sak.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;

@ApplicationScoped
public class OpprettToTrinnsgrunnlag {

    private BeregningTjeneste beregningsgrunnlagTjeneste;
    private TotrinnTjeneste totrinnTjeneste;
    private BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<InformasjonselementerUtleder> informasjonselementer;

    OpprettToTrinnsgrunnlag() {
        // CDI
    }

    @Inject
    public OpprettToTrinnsgrunnlag(BeregningTjeneste beregningsgrunnlagTjeneste,
                                   TotrinnTjeneste totrinnTjeneste,
                                   @Any Instance<InformasjonselementerUtleder> informasjonselementer,
                                   BeregningsgrunnlagVilkårTjeneste beregningsgrunnlagVilkårTjeneste,
                                   InntektArbeidYtelseTjeneste iayTjeneste) {
        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.informasjonselementer = informasjonselementer;
        this.beregningsgrunnlagVilkårTjeneste = beregningsgrunnlagVilkårTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    public void settNyttTotrinnsgrunnlag(Behandling behandling) {
        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = harIayGrunnlag(behandling) ? iayTjeneste.finnGrunnlag(behandling.getId()) : Optional.empty();

        Totrinnresultatgrunnlag totrinnsresultatgrunnlag = new Totrinnresultatgrunnlag(behandling,
            opprettBeregningToTrinn(BehandlingReferanse.fra(behandling)),
            iayGrunnlagOpt.map(InntektArbeidYtelseGrunnlag::getEksternReferanse).orElse(null));

        totrinnTjeneste.lagreNyttTotrinnresultat(behandling, totrinnsresultatgrunnlag);
    }

    private boolean harIayGrunnlag(Behandling behandling) {
        return !(finnTjeneste(behandling.getFagsakYtelseType(), behandling.getType()).utled(behandling.getType()).isEmpty());
    }

    private List<BeregningsgrunnlagToTrinn> opprettBeregningToTrinn(BehandlingReferanse referanse) {

        var skjæringstidspunkter = beregningsgrunnlagVilkårTjeneste.utledPerioderTilVurdering(referanse, false).stream()
            .map(DatoIntervallEntitet::getFomDato).collect(Collectors.toList());

        var beregningsgrunnlag = beregningsgrunnlagTjeneste.hentGrunnlag(referanse, skjæringstidspunkter);
        return beregningsgrunnlag.stream()
            .map(bgg -> bgg.getBeregningsgrunnlag().orElse(null))
            .filter(Objects::nonNull)
            .map(bg -> new BeregningsgrunnlagToTrinn(bg.getSkjæringstidspunkt())).collect(Collectors.toList());

    }

    private InformasjonselementerUtleder finnTjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return InformasjonselementerUtleder.finnTjeneste(informasjonselementer, ytelseType, behandlingType);
    }

}
