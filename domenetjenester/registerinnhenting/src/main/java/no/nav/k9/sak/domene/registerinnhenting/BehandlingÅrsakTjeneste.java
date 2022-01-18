package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandling.BehandlingReferanse;
import no.nav.k9.sak.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.k9.sak.domene.registerinnhenting.impl.behandlingårsak.BehandlingÅrsakUtleder;

@Dependent
public class BehandlingÅrsakTjeneste {

    private Instance<BehandlingÅrsakUtleder> utledere;

    public BehandlingÅrsakTjeneste() {
    }

    @Inject
    public BehandlingÅrsakTjeneste(@Any Instance<BehandlingÅrsakUtleder> utledere) {
        this.utledere = utledere;
    }

    public Set<BehandlingÅrsakType> utledBehandlingÅrsakerBasertPåDiff(BehandlingReferanse behandling, EndringsresultatDiff endringsresultatDiff) {
        // For alle aggregat som har endringer, utled behandlingsårsak.
        return endringsresultatDiff.hentDelresultater().stream()
            .filter(EndringsresultatDiff::erSporedeFeltEndret)
            .map(diff -> finnUtleder(diff.getGrunnlag(), behandling.getFagsakYtelseType())
                .utledBehandlingÅrsaker(behandling, diff.getGrunnlagId1(), diff.getGrunnlagId2()))
            .flatMap(Collection::stream)
            .filter(årsak -> !årsak.equals(BehandlingÅrsakType.UDEFINERT))
            .collect(Collectors.toSet());
    }

    private BehandlingÅrsakUtleder finnUtleder(Class<?> aggregat, FagsakYtelseType ytelseType) {
        var utleder = BehandlingÅrsakUtleder.finnUtleder(utledere, aggregat, ytelseType);
        return utleder.orElseThrow(() -> new IllegalArgumentException("Ingen implementasjoner funnet for BehandlingÅrsakUtleder:" + aggregat + ", ytelseType=" + ytelseType));
    }
}
