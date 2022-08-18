package no.nav.k9.sak.ytelse.opplaeringspenger.prosess.steg;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.OVERGANG_FRA_INFOTRYGD;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OPPLÆRINGSPENGER;

@BehandlingStegRef(value = OVERGANG_FRA_INFOTRYGD)
@FagsakYtelseTypeRef(OPPLÆRINGSPENGER)
@BehandlingTypeRef
@ApplicationScoped
public class OvergangFraInfotrygdSteg implements BehandlingSteg {

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        //TODO
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
