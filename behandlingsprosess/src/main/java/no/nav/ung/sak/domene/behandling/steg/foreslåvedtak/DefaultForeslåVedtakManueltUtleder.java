package no.nav.ung.sak.domene.behandling.steg.foreslåvedtak;

import jakarta.enterprise.context.ApplicationScoped;

import no.nav.ung.kodeverk.behandling.BehandlingType;
import no.nav.ung.sak.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.ung.sak.behandlingslager.behandling.Behandling;

@FagsakYtelseTypeRef
@ApplicationScoped
public class DefaultForeslåVedtakManueltUtleder implements ForeslåVedtakManueltUtleder {

    @Override
    public boolean skalOppretteForeslåVedtakManuelt(Behandling behandling) {
        return BehandlingType.REVURDERING.equals(behandling.getType()) && behandling.erManueltOpprettet();
    }

}
