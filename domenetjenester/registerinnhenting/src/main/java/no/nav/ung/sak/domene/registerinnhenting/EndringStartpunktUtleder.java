package no.nav.ung.sak.domene.registerinnhenting;

import java.util.Optional;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandling.BehandlingReferanse;
import no.nav.ung.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.ung.sak.behandlingslager.hendelser.StartpunktType;

public interface EndringStartpunktUtleder {
    StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);

    default boolean erBehovForStartpunktUtledning(EndringsresultatDiff diff) {
        return diff.erSporedeFeltEndret();
    }

    static Optional<EndringStartpunktUtleder> finnUtleder(Instance<EndringStartpunktUtleder> utledere, Class<?> aggregat, FagsakYtelseType ytelseType) {
        return GrunnlagRef.Lookup.find(EndringStartpunktUtleder.class, utledere, aggregat, ytelseType);
    }
}
