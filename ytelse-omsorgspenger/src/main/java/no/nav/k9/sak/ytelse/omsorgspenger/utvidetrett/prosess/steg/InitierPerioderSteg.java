package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.prosess.steg;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.INIT_PERIODER;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_AO;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_KS;
import static no.nav.k9.kodeverk.behandling.FagsakYtelseType.OMSORGSPENGER_MA;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Samle sammen fakta for fravær.
 */
@ApplicationScoped
@BehandlingStegRef(value = INIT_PERIODER)
@BehandlingTypeRef
@FagsakYtelseTypeRef(OMSORGSPENGER_KS)
@FagsakYtelseTypeRef(OMSORGSPENGER_MA)
@FagsakYtelseTypeRef(OMSORGSPENGER_AO)
public class InitierPerioderSteg implements BehandlingSteg {

    @Inject
    public InitierPerioderSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
