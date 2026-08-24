package no.nav.ung.sak.inngangsvilkår.avklaring;

import jakarta.enterprise.inject.Instance;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef;

/**
 * Vilkårsavklaringer styres av behandlingstriggere representert som {@link BehandlingÅrsakType}.
 * For hvert vilkår skal det finnes en entydig kobling til én årsak.
 * Implementasjoner merkes med {@link BehandlingÅrsakTypeRef} for å angi hvilken
 * {@link BehandlingÅrsakType} de gjelder for.
 */
public interface VilkårsavklaringTjeneste {

    void settAlleAvklaringerTilFerdig(long behandlingId);

    void settVilkårsperioderTilIkkeVurdertForVilkårsavklaringerUnderArbeid(long behandlingId);

    Optional<VilkårsavklaringUnderArbeid> hentSenesteAvklaringUnderArbeid(long behandlingId);

    static List<VilkårsavklaringTjeneste> sortert(Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester) {
        return vilkårsavklaringTjenester.stream().sorted(Comparator.comparing(it -> it.getClass().getName())).toList();
    }

    static Optional<VilkårsavklaringTjeneste> finnForÅrsak(Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester, BehandlingÅrsakType behandlingÅrsakType) {
        return BehandlingÅrsakTypeRef.Lookup.find(VilkårsavklaringTjeneste.class, vilkårsavklaringTjenester, behandlingÅrsakType);
    }
}

