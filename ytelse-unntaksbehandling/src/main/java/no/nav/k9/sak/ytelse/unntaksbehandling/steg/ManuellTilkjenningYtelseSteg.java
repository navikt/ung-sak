package no.nav.k9.sak.ytelse.unntaksbehandling.steg;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef
@BehandlingStegRef(kode = "MANUELL_TILKJENNING_YTELSE")
@BehandlingTypeRef("BT-010")
@ApplicationScoped
public class ManuellTilkjenningYtelseSteg implements BehandlingSteg {

    public ManuellTilkjenningYtelseSteg() {
        // for CDI proxy
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

}
