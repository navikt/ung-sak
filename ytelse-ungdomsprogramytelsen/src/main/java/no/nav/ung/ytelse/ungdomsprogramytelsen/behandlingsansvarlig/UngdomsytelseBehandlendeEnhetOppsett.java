package no.nav.ung.ytelse.ungdomsprogramytelsen.behandlingsansvarlig;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetOppsett;

import java.util.Set;

@FagsakYtelseTypeRef(FagsakYtelseType.UNGDOMSYTELSE)
@ApplicationScoped
public class UngdomsytelseBehandlendeEnhetOppsett implements BehandlendeEnhetOppsett {

    @Override
    public Set<BehandlingDel> deler() {
        return Set.of(BehandlingDel.SENTRAL);
    }

    @Override
    public String behandlingstema(BehandlingDel del) {
        return null;
    }
}
