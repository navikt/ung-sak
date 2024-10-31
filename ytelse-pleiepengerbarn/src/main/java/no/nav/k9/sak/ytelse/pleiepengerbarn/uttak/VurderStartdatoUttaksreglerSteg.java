package no.nav.k9.sak.ytelse.pleiepengerbarn.uttak;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_STARTDATO_UTTAKSREGLER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_NÆRSTÅENDE;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.PLEIEPENGER_SYKT_BARN;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.felles.konfigurasjon.konfig.KonfigVerdi;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@ApplicationScoped
@BehandlingStegRef(value = VURDER_STARTDATO_UTTAKSREGLER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(PLEIEPENGER_SYKT_BARN)
@FagsakYtelseTypeRef(PLEIEPENGER_NÆRSTÅENDE)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
public class VurderStartdatoUttaksreglerSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private AksjonspunktUtlederNyeRegler aksjonspunktUtlederNyeRegler;

    VurderStartdatoUttaksreglerSteg() {
        // for proxy
    }

    @Inject
    public VurderStartdatoUttaksreglerSteg(BehandlingRepository behandlingRepository,
                                           AksjonspunktUtlederNyeRegler aksjonspunktUtlederNyeRegler) {
        this.behandlingRepository = behandlingRepository;
        this.aksjonspunktUtlederNyeRegler = aksjonspunktUtlederNyeRegler;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        // TODO: Dette burde løses i prosessmodell, men lar alle saker som har fått aksjonspunkt gå videre før vi eventuelt løser det der
        if (!behandling.erRevurdering()) {
            return BehandleStegResultat.utførtUtenAksjonspunkter();
        }
        Optional<AksjonspunktDefinisjon> aksjonspunktSetteDatoNyeRegler = aksjonspunktUtlederNyeRegler.utledAksjonspunktDatoForNyeRegler(behandling);
        return aksjonspunktSetteDatoNyeRegler.map(aksjonspunktDefinisjon -> BehandleStegResultat.utførtMedAksjonspunkter(List.of(aksjonspunktDefinisjon))).orElseGet(BehandleStegResultat::utførtUtenAksjonspunkter);
    }

}
