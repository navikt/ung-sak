package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.institusjon;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingskontroll.AksjonspunktResultat;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.Aksjon;

@BehandlingStegRef(value = BehandlingStegType.VURDER_INSTITUSJON_VILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@ApplicationScoped
public class VurderInstitusjonSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private VurderInstitusjonTjeneste vurderInstitusjonTjeneste;

    VurderInstitusjonSteg() {
        // CDI
    }

    @Inject
    public VurderInstitusjonSteg(BehandlingRepositoryProvider repositoryProvider, VurderInstitusjonTjeneste vurderInstitusjonTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vurderInstitusjonTjeneste = vurderInstitusjonTjeneste;
    }

    private static boolean trengerAvklaring(Aksjon aksjon) {
        return !Objects.equals(aksjon, Aksjon.FORTSETT);
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);
        var aksjon = vurderInstitusjonTjeneste.vurder(referanse);

        if (trengerAvklaring(aksjon)) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(
                AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_INSTITUSJON));
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
