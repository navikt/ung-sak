package no.nav.ung.ytelse.aktivitetspenger.del1.steg.foreslåvlkår;

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

import static no.nav.ung.kodeverk.behandling.BehandlingStegType.LOKALKONTOR_FORESLÅ_VILKÅR;

@BehandlingStegRef(value = LOKALKONTOR_FORESLÅ_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class LokalkontorForeslåVilkårSteg implements BehandlingSteg {

    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LokalkontorForeslåVilkårSteg.class);

    private BehandlingAnsvarligRepository behandlingAnsvarligRepository;
    private BehandlingRepository behandlingRepository;
    private List<VilkårsavklaringTjeneste> alleVilkårsavklaringTjenester;

    LokalkontorForeslåVilkårSteg() {
        // for CDI proxy
    }

    @Inject
    public LokalkontorForeslåVilkårSteg(BehandlingAnsvarligRepository behandlingAnsvarligRepository, BehandlingRepository behandlingRepository,
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
                it.getAksjonspunktDefinisjon().getAksjonspunktType().erLokalkontorAksjonspunkt())
            .filter(Aksjonspunkt::isToTrinnsBehandling).toList();

        if (!totrinnAksjonspunkter.isEmpty()) {
            behandlingAnsvarligRepository.setToTrinnsbehandling(behandlingId, BehandlingDel.LOKAL);
            return BehandleStegResultat.utførtMedAksjonspunkter(List.of(AksjonspunktDefinisjon.LOKALKONTOR_FORESLÅR_VILKÅR));
        } else {
            behandlingAnsvarligRepository.nullstillToTrinnsBehandling(behandlingId, BehandlingDel.LOKAL);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (tilSteg != LOKALKONTOR_FORESLÅ_VILKÅR) {
            // I flyten for avslag/opphør er det ikke behandlingstrigger som styrer vurdert periode, men perioden som til enhver tid er foreslått avklart.
            // Det er fordi vi ikke kan gjøre en vurdering før brukeren har blitt varslet om forslaget og har hatt rett til å uttale seg.
            // Når behandlingen hopper tilbake fra foreslå vedtak, er det vilkårsperioden for tilsvarende den avklarte periode som settes til vurdering.
            // Vilkårsvurdering beholdes intakt, slik at saksbehandler kan redigere den foreslåtte vurderingen.
            alleVilkårsavklaringTjenester.forEach(vilkårsavklaringTjeneste ->
                vilkårsavklaringTjeneste.settVilkårsperioderTilIkkeVurdertForVilkårsavklaringerUnderArbeid(kontekst.getBehandlingId())
            );
        }
    }
}
