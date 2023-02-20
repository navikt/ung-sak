package no.nav.k9.sak.ytelse.unntaksbehandling.steg;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.KONTROLLER_FAKTA;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef
@BehandlingStegRef(value = KONTROLLER_FAKTA)
@BehandlingTypeRef(BehandlingType.UNNTAKSBEHANDLING)
@ApplicationScoped
public class KontrollerFaktaUnntaksbehandlingSteg implements BehandlingSteg {

    // Dummy-steg - for å støtte tilbakehopp ved registeroppdateringer

    @Inject
    public KontrollerFaktaUnntaksbehandlingSteg() {
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
