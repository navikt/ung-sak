package no.nav.k9.sak.ytelse.frisinn.uttak;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@BehandlingStegRef(kode = "INIT_PERIODER")
@BehandlingTypeRef
@FagsakYtelseTypeRef("FRISINN")
public class InitierPerioderSteg implements BehandlingSteg {

    @Inject
    public InitierPerioderSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        if(true) throw new UnsupportedOperationException("Ikke implementert steg INIT_PERIODER for FRISINN ennå");

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
