package no.nav.k9.sak.ytelse.omsorgspenger.årskvantum;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.BEKREFT_UTTAK;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@ApplicationScoped
@BehandlingStegRef(stegtype = BEKREFT_UTTAK)
@BehandlingTypeRef
@FagsakYtelseTypeRef(OMSORGSPENGER)
public class BekreftÅrskvantumUttakSteg implements BehandlingSteg {

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // Dette er et dummy steg for at aksjonspunkt 9004 (manuell overstyring) skal kunne kjøres ETTER at uttak er beregnet i foregående steg
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
