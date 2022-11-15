package no.nav.k9.sak.ytelse.opplaeringspenger.inngangsvilkår.gjennomgått;

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
import no.nav.k9.sak.behandlingskontroll.BehandlingStegModell;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepositoryProvider;

@BehandlingStegRef(value = BehandlingStegType.VURDER_GJENNOMGÅTT_OPPLÆRING)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.OPPLÆRINGSPENGER)
@ApplicationScoped
public class GjennomgåOpplæringSteg implements BehandlingSteg {

    private BehandlingRepository behandlingRepository;
    private GjennomgåttOpplæringTjeneste gjennomgåttOpplæringTjeneste;

    GjennomgåOpplæringSteg() {
        // CDI
    }

    @Inject
    public GjennomgåOpplæringSteg(BehandlingRepositoryProvider repositoryProvider,
                                  GjennomgåttOpplæringTjeneste gjennomgåttOpplæringTjeneste) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.gjennomgåttOpplæringTjeneste = gjennomgåttOpplæringTjeneste;
    }

    private boolean trengerAvklaring(Aksjon aksjon) {
        return !Objects.equals(aksjon, Aksjon.FORTSETT);
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {

        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var referanse = BehandlingReferanse.fra(behandling);
        var aksjon = gjennomgåttOpplæringTjeneste.vurder(referanse);

        if (trengerAvklaring(aksjon)) {
            return BehandleStegResultat.utførtMedAksjonspunktResultater(AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.VURDER_GJENNOMGÅTT_OPPLÆRING));
        }

        gjennomgåttOpplæringTjeneste.lagreVilkårsResultat(referanse);

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType førsteSteg, BehandlingStegType sisteSteg) {
        // Do nothing
    }
}
