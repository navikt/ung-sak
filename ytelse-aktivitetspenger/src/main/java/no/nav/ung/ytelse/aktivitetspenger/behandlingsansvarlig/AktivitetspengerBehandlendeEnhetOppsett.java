package no.nav.ung.ytelse.aktivitetspenger.behandlingsansvarlig;

import jakarta.enterprise.context.ApplicationScoped;
import no.nav.ung.kodeverk.behandling.BehandlingDel;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.produksjonsstyring.behandlingenhet.BehandlendeEnhetOppsett;

import java.util.Set;

@FagsakYtelseTypeRef(FagsakYtelseType.AKTIVITETSPENGER)
@ApplicationScoped
public class AktivitetspengerBehandlendeEnhetOppsett implements BehandlendeEnhetOppsett {

    // TODO TSFF-3237: offisielle behandlingstema-koder for aktivitetspenger er ikke avklart. null gir oppslag uten behandlingstema.
    private static final String BEHANDLINGSTEMA_YTELSE = null;
    private static final String BEHANDLINGSTEMA_INNGANGSVILKÅR = null;

    @Override
    public Set<BehandlingDel> deler() {
        return Set.of(BehandlingDel.SENTRAL, BehandlingDel.NAV_KONTOR);
    }

    @Override
    public String behandlingstema(BehandlingDel del) {
        return del == BehandlingDel.SENTRAL ? BEHANDLINGSTEMA_YTELSE : BEHANDLINGSTEMA_INNGANGSVILKÅR;
    }
}
