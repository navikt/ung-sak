package no.nav.k9.sak.domene.behandling.steg.foreslåvedtak;

import javax.enterprise.context.ApplicationScoped;

import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.k9.sak.behandlingslager.behandling.Behandling;

@FagsakYtelseTypeRef
@ApplicationScoped
public class DefaultForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return BehandlingType.REVURDERING.equals(behandling.getType()) && behandling.erManueltOpprettet();
    }

}
