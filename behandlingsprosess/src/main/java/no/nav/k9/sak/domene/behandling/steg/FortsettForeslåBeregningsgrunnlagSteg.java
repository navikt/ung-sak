package no.nav.k9.sak.domene.behandling.steg;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningStegTjeneste;
import no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag.BeregningsgrunnlagSteg;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG)
@BehandlingTypeRef
@ApplicationScoped
public class FortsettForeslåBeregningsgrunnlagSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningStegTjeneste beregningStegTjeneste;

    protected FortsettForeslåBeregningsgrunnlagSteg() {
        // for CDI proxy
    }

    @Inject
    public FortsettForeslåBeregningsgrunnlagSteg(BehandlingRepository behandlingRepository,
                                                 BeregningStegTjeneste beregningStegTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningStegTjeneste = beregningStegTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling);

        List<AksjonspunktResultat> aksjonspunktResultater = beregningStegTjeneste.fortsettBeregning(ref, FORTSETT_FORESLÅ_BEREGNINGSGRUNNLAG);

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
    }

}
