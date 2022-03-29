package no.nav.k9.sak.domene.behandling.steg;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingStegType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.repository.BehandlingRepository;

@BehandlingStegRef(stegtype = BehandlingStegType.START_STEG)
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class StartSteg implements BehandlingSteg {

    @SuppressWarnings("unused")
    private BehandlingRepository behandlingRepository;

    StartSteg() {
        // for CDI proxy
    }

    @Inject
    public StartSteg(BehandlingRepository behandlingRepository) {
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
