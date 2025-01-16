package no.nav.ung.sak.test.util.behandling;

import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;

public record Vilkårsresultat(
    VilkårType type,
    Utfall utfall
) {
}
