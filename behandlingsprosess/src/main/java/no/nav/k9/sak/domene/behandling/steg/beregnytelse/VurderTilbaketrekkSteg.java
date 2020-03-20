package no.nav.k9.sak.domene.behandling.steg.beregnytelse;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandling.aksjonspunkt.AksjonspunktUtlederInput;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.k9.sak.ytelse.beregning.tilbaketrekk.AksjonspunktutlederTilbaketrekk;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;

@BehandlingStegRef(kode = "VURDER_TILBAKETREKK")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef

@ApplicationScoped
public class VurderTilbaketrekkSteg implements BehandlingSteg {

    private AksjonspunktutlederTilbaketrekk aksjonspunktutlederTilbaketrekk;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    VurderTilbaketrekkSteg() {
        // for CDI proxy
    }

    @Inject
    public VurderTilbaketrekkSteg(AksjonspunktutlederTilbaketrekk aksjonspunktutlederTilbaketrekk,
                                  BehandlingRepository behandlingRepository,
                                  SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.aksjonspunktutlederTilbaketrekk = aksjonspunktutlederTilbaketrekk;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        List<AksjonspunktResultat> aksjonspunkter = aksjonspunktutlederTilbaketrekk.utledAksjonspunkterFor(new AksjonspunktUtlederInput(ref));
        return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
        }
}
