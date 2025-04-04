package no.nav.ung.sak.formidling.vedtak;

import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;
import no.nav.ung.kodeverk.vilkår.VilkårType;

public record DetaljertVilkårResultat(Avslagsårsak avslagsårsak, VilkårType vilkårType, Utfall utfall) {
}
