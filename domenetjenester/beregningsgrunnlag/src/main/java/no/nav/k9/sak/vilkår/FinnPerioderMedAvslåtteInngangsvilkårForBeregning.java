package no.nav.k9.sak.vilkår;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.k9.kodeverk.vilkår.VilkårType;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.KantIKantVurderer;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.Vilkårene;
import no.nav.k9.sak.behandlingslager.behandling.vilkår.periode.VilkårPeriode;
import no.nav.k9.sak.domene.typer.tid.DatoIntervallEntitet;

public class FinnPerioderMedAvslåtteInngangsvilkårForBeregning {

    private static final Set<VilkårType> VILKÅR_HVOR_AVSLAG_IKKE_SKAL_FJERNES = java.util.Set.of(
        VilkårType.OPPTJENINGSVILKÅRET,
        VilkårType.OPPTJENINGSPERIODEVILKÅR
    );

    /** Finner periode med avslått inngangsvilkår. Ser bort i fra avslåtte perioder som kun dekker kant-i-kant mellomrom.
     * @param vilkårene Vilkårene på behandlingen
     * @param kantIKantVurderer Kant i kant vurderer for behandlingen som skal vurderes
     * @return Perioder med avslåtte inngangsvilkår
     */
    public static Set<DatoIntervallEntitet> finnPerioderMedAvslåtteInngangsvilkår(Vilkårene vilkårene, KantIKantVurderer kantIKantVurderer) {
        return vilkårene.getVilkårene().stream()
            .filter(v -> !VILKÅR_HVOR_AVSLAG_IKKE_SKAL_FJERNES.contains(v.getVilkårType()))
            .flatMap(v -> v.getPerioder().stream())
            .filter(p -> !erKunKantIKantMellomrom(p, kantIKantVurderer))
            .filter(p -> no.nav.k9.kodeverk.vilkår.Utfall.IKKE_OPPFYLT.equals(p.getUtfall()))
            .map(VilkårPeriode::getPeriode)
            .collect(Collectors.toSet());
    }

    private static boolean erKunKantIKantMellomrom(VilkårPeriode p, KantIKantVurderer kantIKantVurderer) {
        return kantIKantVurderer.erKantIKant(DatoIntervallEntitet.tilOgMed(p.getPeriode().getFomDato().minusDays(1)),
            DatoIntervallEntitet.fraOgMed(p.getPeriode().getTomDato().plusDays(1)));
    }


}
