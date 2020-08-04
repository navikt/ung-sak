package no.nav.k9.sak.domene.registerinnhenting;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.Entity;

import no.nav.k9.kodeverk.behandling.BehandlingÅrsakType;
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
        //For alle aggregat som har endringer, utled behandlingsårsak.
        return endringsresultatDiff.hentDelresultater().stream()
            .filter(EndringsresultatDiff::erSporedeFeltEndret)
            .map(diff -> finnUtleder(diff.getGrunnlag())
                .utledBehandlingÅrsaker(behandling, diff.getGrunnlagId1(), diff.getGrunnlagId2())).flatMap(Collection::stream)
            .filter(årsak -> !årsak.equals(BehandlingÅrsakType.UDEFINERT))
            .collect(Collectors.toSet());
    }

    private BehandlingÅrsakUtleder finnUtleder(Class<?> aggregat) {
        String aggrNavn = aggregat.getSimpleName();
        if(aggregat.isAnnotationPresent(Entity.class)) {
            aggrNavn = aggregat.getAnnotation(Entity.class).name();
        }

        Instance<BehandlingÅrsakUtleder> selected = utledere.select(new GrunnlagRef.GrunnlagRefLiteral(aggrNavn));
        if (selected.isAmbiguous()) {
            throw new IllegalArgumentException("Mer enn en implementasjon funnet for BehandlingÅrsakUtleder:" + aggrNavn);
        } else if (selected.isUnsatisfied()) {
            throw new IllegalArgumentException("Ingen implementasjoner funnet for BehandlingÅrsakUtleder:" + aggrNavn);
        }
        BehandlingÅrsakUtleder minInstans = selected.get();
        if (minInstans.getClass().isAnnotationPresent(Dependent.class)) {
            throw new IllegalStateException("Kan ikke ha @Dependent scope bean ved Instance lookup dersom en ikke også håndtere lifecycle selv: " + minInstans.getClass());
        }
        return selected.get();
    }
}
