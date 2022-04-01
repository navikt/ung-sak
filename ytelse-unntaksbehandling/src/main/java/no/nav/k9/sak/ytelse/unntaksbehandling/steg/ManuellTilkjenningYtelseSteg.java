package no.nav.k9.sak.ytelse.unntaksbehandling.steg;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.MANUELL_TILKJENNING_YTELSE;
import static no.nav.k9.kodeverk.behandling.BehandlingType.UNNTAKSBEHANDLING;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.BehandleStegResultat;
import no.nav.k9.sak.behandlingskontroll.BehandlingSteg;
import no.nav.k9.sak.behandlingskontroll.BehandlingStegRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingTypeRef;
import no.nav.k9.sak.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;

@FagsakYtelseTypeRef
@BehandlingStegRef(stegtype = MANUELL_TILKJENNING_YTELSE)
@BehandlingTypeRef(UNNTAKSBEHANDLING)
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
