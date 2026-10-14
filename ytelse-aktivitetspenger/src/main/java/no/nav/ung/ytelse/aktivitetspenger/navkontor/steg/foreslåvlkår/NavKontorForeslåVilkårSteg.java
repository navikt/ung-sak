package no.nav.ung.ytelse.aktivitetspenger.navkontor.steg.foreslåvlkår;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.BehandlingStegType;
import no.nav.ung.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.ung.sak.behandlingskontroll.*;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;
import no.nav.ung.sak.behandlingslager.behandling.aksjonspunkt.Aksjonspunkt;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingAnsvarligRepository;
import no.nav.ung.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.ung.sak.inngangsvilkår.avklaring.VilkårsavklaringTjeneste;
import org.slf4j.Logger;

import java.util.List;

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.NAV_KONTOR_FORESLÅ_VILKÅR;

@BehandlingStegRef(value = NAV_KONTOR_FORESLÅ_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class NavKontorForeslåVilkårSteg implements BehandlingSteg {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(NavKontorForeslåVilkårSteg.class);

    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;
    private BehandlingRepository behandlingRepository;
    private List<VilkårsavklaringTjeneste> alleVilkårsavklaringTjenester;

    NavKontorForeslåVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public NavKontorForeslåVilkårSteg(BehandlingAnsvarligRepository behandlingAnsvarligRepository, BehandlingRepository behandlingRepository,
                                      Instance<VilkårsavklaringTjeneste> alleVilkårsavklaringTjenester) {
        this.behandlingAnsvarligRepository = behandlingAnsvarligRepository;
        this.behandlingRepository = behandlingRepository;
        this.alleVilkårsavklaringTjenester = VilkårsavklaringTjeneste.sortert(alleVilkårsavklaringTjenester);
    }


    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        long behandlingId = kontekst.getBehandlingId();

        Behandling behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var totrinnAksjonspunkter = behandling.getAksjonspunkter().stream()
            .filter(it -> it.getAksjonspunktDefinisjon().getAksjonspunktType() != null &&
                it.getAksjonspunktDefinisjon().getAksjonspunktType().erNavKontorAksjonspunkt())
            .filter(Aksjonspunkt::isToTrinnsBehandling).toList();

        if (!totrinnAksjonspunkter.isEmpty()) {
            behandlingAnsvarligRepository.setToTrinnsbehandling(behandlingId, BehandlingDel.NAV_KONTOR);
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.NAV_KONTOR_FORESLÅR_VILKÅR));
        } else {
            behandlingAnsvarligRepository.nullstillToTrinnsBehandling(behandlingId, BehandlingDel.NAV_KONTOR);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (tilSteg != NAV_KONTOR_FORESLÅ_VILKÅR) {
            // I flyten for avslag/opphør er det ikke behandlingstrigger som styrer vurdert periode, men perioden som til enhver tid er foreslått avklart.
            // Det er fordi vi ikke kan gjøre en vurdering før brukeren har blitt varslet om forslaget og har hatt rett til å uttale seg.
            // Når behandlingen hopper tilbake fra foreslå vedtak, er det vilkårsperioden for tilsvarende den avklarte periode som settes til vurdering.
            // Vilkårsvurdering beholdes intakt, slik at saksbehandler kan redigere den foreslåtte vurderingen.
            alleVilkårsavklaringTjenester.forEach(vilkårsavklaringTjeneste ->
                vilkårsavklaringTjeneste.settVilkårsperioderTilIkkeVurdertForForeslåtteAvklaringer(kontekst.getBehandlingId())
            );
        }
    }
}
