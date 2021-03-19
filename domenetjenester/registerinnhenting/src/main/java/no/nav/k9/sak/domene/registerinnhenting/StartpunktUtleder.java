package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Optional;

import javax.enterprise.inject.Instance;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.behandlingslager.hendelser.StartpunktType;

public interface StartpunktUtleder {
    StartpunktType utledStartpunkt(BehandlingReferanse ref, Object grunnlagId1, Object grunnlagId2);

    default boolean erBehovForStartpunktUtledning(EndringsresultatDiff diff) {
        return diff.erSporedeFeltEndret();
    }

    static Optional<StartpunktUtleder> finnUtleder(Instance<StartpunktUtleder> utledere, Class<?> aggregat, FagsakYtelseType ytelseType) {
        return GrunnlagRef.Lookup.find(StartpunktUtleder.class, utledere, aggregat, ytelseType);
    }

    static Optional<StartpunktUtleder> finnUtleder(Instance<StartpunktUtleder> utledere, String aggregat, FagsakYtelseType ytelseType) {
        return GrunnlagRef.Lookup.find(StartpunktUtleder.class, utledere, aggregat, ytelseType);
    }
}
