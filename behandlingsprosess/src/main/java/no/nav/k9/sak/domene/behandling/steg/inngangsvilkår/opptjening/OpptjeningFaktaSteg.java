package no.nav.k9.sak.domene.behandling.steg.inngangsvilkår.opptjening;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

/**
 * Steg 81 - Kontroller fakta for opptjening
 */
@BehandlingStegRef(kode = "VURDER_OPPTJ_FAKTA")
@BehandlingTypeRef
@FagsakYtelseTypeRef
@ApplicationScoped
public class OpptjeningFaktaSteg implements BehandlingSteg {

    @Inject
    OpptjeningFaktaSteg() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        // TODO: Steg ikke lenger i bruk - fjerne steg fra prosesmodeller
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
