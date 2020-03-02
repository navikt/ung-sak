package no.nav.foreldrepenger.behandling.revurdering;

import java.util.Collection;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.BehandlingType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;

public abstract class RevurderingEndringBasertPåKonsekvenserForYtelsen implements RevurderingEndring {

    public static final String UTVIKLERFEIL_INGEN_ENDRING_SAMMEN = "Utviklerfeil: Det skal ikke være mulig å ha INGEN_ENDRING sammen med andre konsekvenser. BehandlingId: ";

    @Override
    public boolean erRevurderingMedUendretUtfall(BehandlingReferanse ref, Collection<KonsekvensForYtelsen> konsekvenserForYtelsen, BehandlingResultatType nyResultatType) {
        return erRevurderingMedUendretUtfall(ref, konsekvenserForYtelsen);
    }

    @Override
    public boolean erRevurderingMedUendretUtfall(BehandlingReferanse ref, Collection<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        if (!BehandlingType.REVURDERING.equals(ref.getBehandlingType())) {
            return false;
        }
        boolean ingenKonsekvensForYtelsen = konsekvenserForYtelsen.contains(KonsekvensForYtelsen.INGEN_ENDRING);
        if (ingenKonsekvensForYtelsen && konsekvenserForYtelsen.size() > 1) {
            throw new IllegalStateException(UTVIKLERFEIL_INGEN_ENDRING_SAMMEN + ref.getBehandlingId());
        }
        return ingenKonsekvensForYtelsen;
    }
}
