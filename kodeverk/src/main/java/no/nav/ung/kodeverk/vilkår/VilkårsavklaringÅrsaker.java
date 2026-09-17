package no.nav.ung.kodeverk.vilkår;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Koblingen mellom et vilkår og behandlingsårsaken som utløser en vilkårsavklaring for det.
 * Koblingen er 1:1 – se {@code VilkårsavklaringTjeneste}, der {@code BehandlingÅrsakTypeRef.Lookup}
 * krever at hver årsak har nøyaktig én tjeneste.
 */
public final class VilkårsavklaringÅrsaker {

    private static final Map<VilkårType, BehandlingÅrsakType> AVKLARINGSÅRSAK_PER_VILKÅR = Map.of(
        VilkårType.BOSTEDSVILKÅR, BehandlingÅrsakType.ENDRET_BOSTED,
        VilkårType.BISTANDSVILKÅR, BehandlingÅrsakType.ENDRET_BISTANDSBEHOV,
        VilkårType.ANDRE_LIVSOPPHOLDSYTELSER_VILKÅR, BehandlingÅrsakType.ENDRET_LIVSOPPHOLDSYTELSE
    );

    private VilkårsavklaringÅrsaker() {
    }

    public static Map<VilkårType, BehandlingÅrsakType> alle() {
        return AVKLARINGSÅRSAK_PER_VILKÅR;
    }

    public static Optional<BehandlingÅrsakType> avklaringsårsakFor(VilkårType vilkårType) {
        return Optional.ofNullable(AVKLARINGSÅRSAK_PER_VILKÅR.get(vilkårType));
    }

    public static Set<BehandlingÅrsakType> alleÅrsaker() {
        return Set.copyOf(AVKLARINGSÅRSAK_PER_VILKÅR.values());
    }
}
