package no.nav.foreldrepenger.behandling.revurdering;

import java.util.Collection;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.kodeverk.behandling.BehandlingResultatType;
import no.nav.k9.kodeverk.behandling.KonsekvensForYtelsen;

public interface RevurderingEndring {

    boolean erRevurderingMedUendretUtfall(BehandlingReferanse ref, Collection<KonsekvensForYtelsen> konsekvenserForYtelsen, BehandlingResultatType nyResultatType);

    /**
     * Tjeneste som vurderer om revurderingen har endret utrfall i forhold til original behandling
     */
    boolean erRevurderingMedUendretUtfall(BehandlingReferanse ref, Collection<KonsekvensForYtelsen> konsekvenserForYtelsen);
}
