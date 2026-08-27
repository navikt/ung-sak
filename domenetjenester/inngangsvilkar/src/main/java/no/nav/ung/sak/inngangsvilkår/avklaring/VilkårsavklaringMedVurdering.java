package no.nav.ung.sak.inngangsvilkår.avklaring;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.kodeverk.vilkår.VilkårType;
import no.nav.ung.sak.behandlingslager.inngangsvilkår.VilkårsvurderingResultatPeriode;

/**
 * Kobler sammen vilkårsavklaringen (som styres av {@link BehandlingÅrsakType}) med vilkårsvurderingen
 * (som er knyttet til {@link VilkårType}) for en gitt periode.
 *
 * @param vilkårsvurdering kan være null dersom vilkåret ikke er vurdert i perioden
 */
public record VilkårsavklaringMedVurdering(
    VilkårType vilkårType,
    BehandlingÅrsakType behandlingÅrsakType,
    Vilkårsavklaring vilkårsavklaring,
    VilkårsvurderingResultatPeriode vilkårsvurdering
) {

    public boolean harVilkårsAvklaring() {
        return vilkårsavklaring != null;
    }
}
