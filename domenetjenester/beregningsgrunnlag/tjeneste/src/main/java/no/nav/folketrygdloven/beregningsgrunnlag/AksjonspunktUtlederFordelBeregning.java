package no.nav.folketrygdloven.beregningsgrunnlag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.beregningsgrunnlag.gradering.AktivitetGradering;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste;
import no.nav.folketrygdloven.beregningsgrunnlag.kontrollerfakta.FordelBeregningsgrunnlagTjeneste.VurderManuellBehandling;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktDefinisjon;
import no.nav.folketrygdloven.beregningsgrunnlag.output.BeregningAksjonspunktResultat;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.domene.iay.modell.Inntektsmelding;

@ApplicationScoped
class AksjonspunktUtlederFordelBeregning {

    private FordelBeregningsgrunnlagTjeneste fordelBeregningsgrunnlagTjeneste;

    AksjonspunktUtlederFordelBeregning() {
        // for CDI proxy
    }

    @Inject
    public AksjonspunktUtlederFordelBeregning(FordelBeregningsgrunnlagTjeneste fordelBeregningsgrunnlagTjeneste) {
        this.fordelBeregningsgrunnlagTjeneste = fordelBeregningsgrunnlagTjeneste;
    }

    protected List<BeregningAksjonspunktResultat> utledAksjonspunkterFor(BehandlingReferanse ref,
                                                                         BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag,
                                                                         AktivitetGradering aktivitetGradering,
                                                                         Collection<Inntektsmelding> inntektsmeldinger) {
        List<BeregningAksjonspunktResultat> aksjonspunktResultater = new ArrayList<>();
        if (utledAksjonspunktFordelBG(ref, beregningsgrunnlagGrunnlag, aktivitetGradering, inntektsmeldinger).isPresent()) {
            BeregningAksjonspunktResultat aksjonspunktResultat = BeregningAksjonspunktResultat.opprettFor(BeregningAksjonspunktDefinisjon.FORDEL_BEREGNINGSGRUNNLAG);
            aksjonspunktResultater.add(aksjonspunktResultat);
        }
        return aksjonspunktResultater;
    }

    private Optional<VurderManuellBehandling> utledAksjonspunktFordelBG(@SuppressWarnings("unused") BehandlingReferanse ref,
                                                                        BeregningsgrunnlagGrunnlagEntitet beregningsgrunnlagGrunnlag,
                                                                        AktivitetGradering aktivitetGradering,
                                                                        Collection<Inntektsmelding> inntektsmeldinger) {
        BeregningsgrunnlagEntitet beregningsgrunnlag = beregningsgrunnlagGrunnlag.getBeregningsgrunnlag()
            .orElseThrow(() -> new IllegalStateException("Utviklerfeil: Mangler beregningsgrunnlagGrunnlag"));
        return fordelBeregningsgrunnlagTjeneste.vurderManuellBehandling(beregningsgrunnlag, beregningsgrunnlagGrunnlag.getGjeldendeAktiviteter(), aktivitetGradering, inntektsmeldinger);
    }
}
