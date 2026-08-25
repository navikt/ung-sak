package no.nav.ung.ytelse.aktivitetspenger.testdata;

import no.nav.ung.kodeverk.vilkår.Avslagsårsak;
import no.nav.ung.kodeverk.vilkår.Utfall;

public record VilkårUtfall(Utfall utfall, Avslagsårsak avslagsårsak, String fritekstBrev) {

    public static VilkårUtfall oppfylt() {
        return new VilkårUtfall(Utfall.OPPFYLT, null, null);
    }

    public static VilkårUtfall avslått(Avslagsårsak avslagsårsak) {
        return avslått(avslagsårsak, null);
    }

    public static VilkårUtfall avslått(Avslagsårsak avslagsårsak, String fritekstBrev) {
        return new VilkårUtfall(Utfall.IKKE_OPPFYLT, avslagsårsak, fritekstBrev);
    }
}
