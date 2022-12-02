package no.nav.k9.sak.domene.behandling.steg.beregningsgrunnlag;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLER_FAKTA_BEREGNING;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.aksjonspunkt.AksjonspunktKontrollRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = KONTROLLER_FAKTA_BEREGNING)
@BehandlingTypeRef
@ApplicationScoped
public class KontrollerFaktaBeregningSteg implements BeregningsgrunnlagSteg {

    private BehandlingRepository behandlingRepository;
    private BeregningStegTjeneste beregningStegTjeneste;
    private AksjonspunktKontrollRepository aksjonspunktKontrollRepository;

    protected KontrollerFaktaBeregningSteg() {
        // for CDI proxy
    }

    @Inject
    public KontrollerFaktaBeregningSteg(BehandlingRepository behandlingRepository,
                                        BeregningStegTjeneste beregningStegTjeneste,
                                        AksjonspunktKontrollRepository aksjonspunktKontrollRepository) {
        this.behandlingRepository = behandlingRepository;
        this.beregningStegTjeneste = beregningStegTjeneste;
        this.aksjonspunktKontrollRepository = aksjonspunktKontrollRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);
        List<AksjonspunktResultat> aksjonspunktResultater = beregningStegTjeneste.fortsettBeregning(ref, KONTROLLER_FAKTA_BEREGNING);

        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunktResultater);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());


        var harAPSomIkkeErOverstyring = behandling.getAksjonspunkter()
            .stream()
            .filter(a -> a.getBehandlingStegFunnet() != null && a.getBehandlingStegFunnet().equals(KONTROLLER_FAKTA_BEREGNING))
            .anyMatch(a -> !a.erManueltOpprettet());

        var overstyringAksjonspunkt = behandling.getAksjonspunkter()
            .stream()
            .filter(a -> a.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.OVERSTYRING_AV_BEREGNINGSGRUNNLAG))
            .filter(a -> a.getStatus().equals(AksjonspunktStatus.OPPRETTET))
            .findFirst();

        if (overstyringAksjonspunkt.isPresent() && harAPSomIkkeErOverstyring) {
            // Hack for å slippe å bekrefte to ganger ved overstyring. Ved hopp tilbake dersom vi allerede har aksjonspunkt i steget vil vi uansett
            // stoppe
            aksjonspunktKontrollRepository.setTilUtført(overstyringAksjonspunkt.get(), overstyringAksjonspunkt.get().getBegrunnelse());
        }
    }

}
