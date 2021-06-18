package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import no.nav.k9.sak.behandlingskontroll.*;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.k9.sak.ytelse.omsorgspenger.årskvantum.tjenester.ÅrskvantumTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@BehandlingStegRef(kode = "BEKREFT_UTTAK")
@BehandlingTypeRef
@FagsakYtelseTypeRef("OMP")
public class BekreftÅrskvantumUttakSteg implements BehandlingSteg {

    private static final Logger log = LoggerFactory.getLogger(BekreftÅrskvantumUttakSteg.class);

    private BehandlingRepository behandlingRepository;
    private ÅrskvantumTjeneste årskvantumTjeneste;

    BekreftÅrskvantumUttakSteg() {
        // for proxy
    }

    @Inject
    public BekreftÅrskvantumUttakSteg(BehandlingRepository behandlingRepository,
                                     ÅrskvantumTjeneste årskvantumTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.årskvantumTjeneste = årskvantumTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // Dette er et dummy steg for at aksjonspunkt 9004 (manuell overstyring) skal kunne kjøres ETTER at uttak er beregnet i foregående steg
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
