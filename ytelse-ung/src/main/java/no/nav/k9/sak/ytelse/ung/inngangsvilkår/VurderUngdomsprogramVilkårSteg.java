package no.nav.k9.sak.ytelse.ung.inngangsvilkår;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingskontroll.*;

import static no.nav.k9.kodeverk.behandling.BehandlingStegType.VURDER_UNGDOMSPROGRAMVILKÅR;

@BehandlingStegRef(value = VURDER_UNGDOMSPROGRAMVILKÅR)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class VurderUngdomsprogramVilkårSteg implements BehandlingSteg {
    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }
}
