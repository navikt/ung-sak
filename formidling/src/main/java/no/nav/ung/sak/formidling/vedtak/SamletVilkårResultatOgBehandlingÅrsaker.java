package no.nav.ung.sak.formidling.vedtak;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * Intermidiate object for vilkårresultat and behandling årsaker primært brukt for tidslinje
 *
 */
public record SamletVilkårResultatOgBehandlingÅrsaker(List<DetaljertVilkårResultat> vilkårsresultater,
                                                      Set<BehandlingÅrsakType> behandlingÅrsaker) {

    public Set<Utfall> utfall() {
        return vilkårsresultater.stream().map(DetaljertVilkårResultat::utfall).collect(Collectors.toSet());
    }

    public Set<DetaljertVilkårResultat> avslåtteVilkår() {
        return vilkårsresultater.stream()
            .filter(it -> it.utfall() == Utfall.IKKE_OPPFYLT)
            .collect(Collectors.toSet());
    }

    public Set<DetaljertVilkårResultat> ikkeVurderteVilkår() {
        return vilkårsresultater.stream().filter(it -> it.utfall() == Utfall.IKKE_VURDERT).collect(Collectors.toSet());
    }

}
