package no.nav.k9.sak.domene.behandling.steg.kompletthet;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandling.Skjæringstidspunkt;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.kompletthet.Kompletthetsjekker;
import no.nav.k9.sak.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@BehandlingStegRef(kode = "POSTCONDITION_KOMPLETTHET")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class PostconditionKompletthetSteg implements BehandlingSteg {

    private Instance<Kompletthetsjekker> kompletthetsjekkerInstances;
    private BehandlingRepository behandlingRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    PostconditionKompletthetSteg() {
    }

    @Inject
    public PostconditionKompletthetSteg(@Any Instance<Kompletthetsjekker> kompletthetsjekker,
                                        BehandlingRepositoryProvider provider,
                                        SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {
        this.kompletthetsjekkerInstances = kompletthetsjekker;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = provider.getBehandlingRepository();
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        Behandling behandling = behandlingRepository.hentBehandling(behandlingId);
        if (behandling.erManueltOpprettet() && behandling.erRevurdering()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }

        Skjæringstidspunkt skjæringstidspunkter = skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId);
        BehandlingReferanse ref = BehandlingReferanse.fra(behandling, skjæringstidspunkter);
        Kompletthetsjekker kompletthetsjekker = getKompletthetsjekker(ref);

        if (kompletthetsjekker.ingenSøknadsperioder(ref)) {
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.BEKREFT_HENLEGGELSE_UKOMPLETT_SØKNADSGRUNNLAG));
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private Kompletthetsjekker getKompletthetsjekker(BehandlingReferanse ref) {
        return BehandlingTypeRef.Lookup.find(Kompletthetsjekker.class, kompletthetsjekkerInstances, ref.getFagsakYtelseType(), ref.getBehandlingType())
            .orElseThrow(() -> new UnsupportedOperationException("Har ikke " + Kompletthetsjekker.class.getSimpleName() + " for " + ref.getBehandlingUuid()));
    }
}
