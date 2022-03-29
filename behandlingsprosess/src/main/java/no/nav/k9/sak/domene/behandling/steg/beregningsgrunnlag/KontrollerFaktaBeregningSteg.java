package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;

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

@FagsakYtelseTypeRef("*")
@BehandlingStegRef(stegtype = KONTROLLER_FAKTA_BEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerFaktaBeregningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningStegTjeneste beregningStegTjeneste;

    protected KontrollerFaktaBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public KontrollerFaktaBeregningSteg(BehandlingRepository behandlingRepository,
                                        BeregningStegTjeneste beregningStegTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningStegTjeneste = beregningStegTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        List<AksjonspunktResultat> aksjonspunktResultater = beregningStegTjeneste.fortsettBeregning(ref, KONTROLLER_FAKTA_BEREGNING);

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
    }

}
