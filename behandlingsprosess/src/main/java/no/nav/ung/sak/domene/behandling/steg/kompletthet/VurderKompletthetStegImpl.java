package no.nav.ung.sak.domene.behandling.steg.kompletthet;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.VURDER_KOMPLETTHET;
import static no.nav.ung.kodeverk.behandling.BehandlingType.FØRSTEGANGSSØKNAD;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.AUTO_VENTER_PÅ_KOMPLETT_SØKNAD;
import static no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon.VENT_PGA_FOR_TIDLIG_SØKNAD;
import static no.nav.ung.sak.domene.behandling.steg.kompletthet.VurderKompletthetStegFelles.autopunktAlleredeUtført;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandling.Skjæringstidspunkt;
import no.nav.ung.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.ung.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.ung.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.ung.sak.kompletthet.KompletthetResultat;
import no.nav.ung.sak.kompletthet.Kompletthetsjekker;
import no.nav.ung.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(value = VURDER_KOMPLETTHET)
@BehandlingTypeRef(FØRSTEGANGSSØKNAD)
@FagsakYtelseTypeRef
@ApplicationScoped
public class VurderKompletthetStegImpl implements VurderKompletthetSteg {

    private Instance<Kompletthetsjekker> kompletthetsjekkerInstances;
    private BehandlingRepository behandlingRepository;
    private VurderKompletthetStegFelles vurderKompletthetStegFelles;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    VurderKompletthetStegImpl() {
    }

    @Inject
    public VurderKompletthetStegImpl(@Any Instance<Kompletthetsjekker> kompletthetsjekker,
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
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);

        Kompletthetsjekker kompletthetsjekker = getKompletthetsjekker(ref);
        KompletthetResultat søknadMottatt = kompletthetsjekker.vurderSøknadMottattForTidlig(ref);
        if (!søknadMottatt.erOppfylt()) {
            return vurderKompletthetStegFelles.evaluerUoppfylt(søknadMottatt, VENT_PGA_FOR_TIDLIG_SØKNAD);
        }

        KompletthetResultat forsendelseMottatt = kompletthetsjekker.vurderForsendelseKomplett(ref);
        if (!forsendelseMottatt.erOppfylt() && !autopunktAlleredeUtført(AUTO_VENTER_PÅ_KOMPLETT_SØKNAD, behandling)) {
            return vurderKompletthetStegFelles.evaluerUoppfylt(forsendelseMottatt, AUTO_VENTER_PÅ_KOMPLETT_SØKNAD);
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Kompletthetsjekker getKompletthetsjekker(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(Kompletthetsjekker.class, kompletthetsjekkerInstances, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + Kompletthetsjekker.class.getSimpleName() + " for " + ref.getBehandlingUuid()));
    }
}
