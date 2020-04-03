package no.nav.k9.sak.domene.behandling.steg.kompletthet;

import static no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;
import static no.nav.k9.sak.domene.behandling.steg.kompletthet.VurderKompletthetStegFelles.autopunktAlleredeUtført;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.kompletthet.KompletthetResultat;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "VURDERKOMPLETT")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderKompletthetRevurderingStegImpl implements VurderKompletthetSteg {

    private Instance<Kompletthetsjekker> kompletthetsjekkerInstances;
    private BehandlingRepository behandlingRepository;
    private VurderKompletthetStegFelles vurderKompletthetStegFelles;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    VurderKompletthetRevurderingStegImpl() {
    }

    @Inject
    public VurderKompletthetRevurderingStegImpl(@Any Instance<Kompletthetsjekker> kompletthetsjekker,
                                                BehandlingRepositoryProvider provider,
                                                VurderKompletthetStegFelles vurderKompletthetStegFelles,
                                                SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.kompletthetsjekkerInstances = kompletthetsjekker;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
        this.vurderKompletthetStegFelles = vurderKompletthetStegFelles;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        KompletthetResultat kompletthetResultat = getKompletthetsjekker(ref).vurderForsendelseKomplett(ref);
        if (!kompletthetResultat.erOppfylt() && !autopunktAlleredeUtført(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, behandling)) {
            return vurderKompletthetStegFelles.evaluerUoppfylt(kompletthetResultat, AUTO_VENTER_PÅ_KOMPLETT_SØKNAD);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Kompletthetsjekker getKompletthetsjekker(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(Kompletthetsjekker.class, kompletthetsjekkerInstances, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + Kompletthetsjekker.class.getSimpleName() + " for " + ref));
    }
}
