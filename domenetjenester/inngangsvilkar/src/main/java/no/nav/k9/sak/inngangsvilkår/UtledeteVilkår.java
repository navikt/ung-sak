package no.nav.k9.sak.inngangsvilkår;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import no.nav.k9.kodeverk.vilkår.VilkårType;

public class UtledeteVilkår {

    // Avklart betinget vilkår. Kan initielt settes til null, men må avklares før steg Kontroller fakta er ferdig.
    private VilkårType avklartBetingetVilkårType;

    // I tillegg til betinget vilkår vil det følge tilhørende vilkår
    private List<VilkårType> tilhørendeVilkår;

    public UtledeteVilkår(VilkårType betingetVilkårType, List<VilkårType> tilhørendeVilkårTyper) {
        avklartBetingetVilkårType = betingetVilkårType;
        tilhørendeVilkår = tilhørendeVilkårTyper;
    }

    public Set<VilkårType> getAlleAvklarte() {
        Set<VilkårType> avklarteVilkår = new TreeSet<>();
        // Betinget vilkår kan være uavklart inntil aksjonspunkt er løst.
        if (avklartBetingetVilkårType != null) {
            avklarteVilkår.add(avklartBetingetVilkårType);
        }
        avklarteVilkår.addAll(tilhørendeVilkår);
        return avklarteVilkår;
    }
}
