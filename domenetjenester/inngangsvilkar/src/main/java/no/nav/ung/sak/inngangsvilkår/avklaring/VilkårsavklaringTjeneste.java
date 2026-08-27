package no.nav.ung.sak.inngangsvilkår.avklaring;

import jakarta.enterprise.inject.Instance;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingskontroll.BehandlingÅrsakTypeRef;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Vilkårsavklaringer styres av behandlingstriggere representert som {@link BehandlingÅrsakType}.
 * For hvert vilkår skal det finnes en entydig kobling til én årsak.
 * Implementasjoner merkes med {@link BehandlingÅrsakTypeRef} for å angi hvilken
 * {@link BehandlingÅrsakType} de gjelder for.
 */
public interface VilkårsavklaringTjeneste {

    void settAlleAvklaringerTilFerdig(long behandlingId);

    void settVilkårsperioderTilIkkeVurdertForVilkårsavklaringerUnderArbeid(long behandlingId);

    // Henter seneste vilkårsavklaring for behandling. Hvis det er flere vilkårsavklaringer lagret samtidig velges den med senest fom.
    Optional<Vilkårsavklaring> hentSenesteAvklaringForBehandling(long behandlingId);

    static List<VilkårsavklaringTjeneste> sortert(Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester) {
        return vilkårsavklaringTjenester.stream().sorted(Comparator.comparing(it -> it.getClass().getName())).toList();
    }

    static Optional<VilkårsavklaringTjeneste> finnForÅrsak(Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester, BehandlingÅrsakType behandlingÅrsakType) {
        return BehandlingÅrsakTypeRef.Lookup.find(VilkårsavklaringTjeneste.class, vilkårsavklaringTjenester, behandlingÅrsakType);
    }
}

