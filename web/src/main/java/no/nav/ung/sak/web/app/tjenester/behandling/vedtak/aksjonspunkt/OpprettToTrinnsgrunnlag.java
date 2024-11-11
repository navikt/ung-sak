package no.nav.ung.sak.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.ung.sak.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.ung.sak.domene.registerinnhenting.InformasjonselementerUtleder;
import no.nav.ung.sak.produksjonsstyring.totrinn.TotrinnTjeneste;
import no.nav.ung.sak.produksjonsstyring.totrinn.Totrinnresultatgrunnlag;

@ApplicationScoped
public class OpprettToTrinnsgrunnlag {

    private TotrinnTjeneste totrinnTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private Instance<InformasjonselementerUtleder> informasjonselementer;

    OpprettToTrinnsgrunnlag() {
        // CDI
    }

    @Inject
    public OpprettToTrinnsgrunnlag(TotrinnTjeneste totrinnTjeneste,
                                   @Any Instance<InformasjonselementerUtleder> informasjonselementer,
                                   InntektArbeidYtelseTjeneste iayTjeneste) {
        this.informasjonselementer = informasjonselementer;
        this.iayTjeneste = iayTjeneste;
        this.totrinnTjeneste = totrinnTjeneste;
    }

    public void settNyttTotrinnsgrunnlag(Behandling behandling) {
        Optional<InntektArbeidYtelseGrunnlag> iayGrunnlagOpt = harIayGrunnlag(behandling) ? iayTjeneste.finnGrunnlag(behandling.getId()) : Optional.empty();

        Totrinnresultatgrunnlag totrinnsresultatgrunnlag = new Totrinnresultatgrunnlag(behandling,
            iayGrunnlagOpt.map(InntektArbeidYtelseGrunnlag::getEksternReferanse).orElse(null));

        totrinnTjeneste.lagreNyttTotrinnresultat(behandling, totrinnsresultatgrunnlag);
    }

    private boolean harIayGrunnlag(Behandling behandling) {
        return !(finnTjeneste(behandling.getFagsakYtelseType(), behandling.getType()).utled(behandling.getType()).isEmpty());
    }

    private InformasjonselementerUtleder finnTjeneste(FagsakYtelseType ytelseType, BehandlingType behandlingType) {
        return InformasjonselementerUtleder.finnTjeneste(informasjonselementer, ytelseType, behandlingType);
    }

}
