package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.Utfall;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record DetaljertResultatPeriodeGrunnlag(List<DetaljertVilkårResultat> vilkårsresultater,
                                               Set<BehandlingÅrsakType> behandlingÅrsaker,
                                               boolean tilVurdering) {

    public Set<DetaljertVilkårResultat> avslåtteVilkår() {
        return vilkårsresultater.stream()
            .filter(it -> it.utfall() == Utfall.IKKE_OPPFYLT)
            .collect(Collectors.toSet());
    }

    public Set<DetaljertVilkårResultat> ikkeVurderteVilkår() {
        return vilkårsresultater.stream()
            .filter(it -> it.utfall() == Utfall.IKKE_VURDERT)
            .collect(Collectors.toSet());
    }

}

