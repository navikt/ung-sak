package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Optional;

import javax.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

public interface EndringStartpunktUtleder {
    StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);

    default boolean erBehovForStartpunktUtledning(EndringsresultatDiff diff) {
        return diff.erSporedeFeltEndret();
    }

    static Optional<EndringStartpunktUtleder> finnUtleder(Instance<EndringStartpunktUtleder> utledere, Class<?> aggregat, FagsakYtelseType ytelseType) {
        return GrunnlagRef.Lookup.find(EndringStartpunktUtleder.class, utledere, aggregat, ytelseType);
    }

    static Optional<EndringStartpunktUtleder> finnUtleder(Instance<EndringStartpunktUtleder> utledere, String aggregat, FagsakYtelseType ytelseType) {
        return GrunnlagRef.Lookup.find(EndringStartpunktUtleder.class, utledere, aggregat, ytelseType);
    }
}
