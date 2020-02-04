package no.nav.foreldrepenger.inngangsvilkaar.impl;

import java.util.ArrayList;
import java.util.List;

import no.nav.k9.kodeverk.vilkår.VilkårType;

public class UtledeteVilkår {

     // Avklart betinget vilkår. Kan initielt settes til null, men må avklares før steg Kontroller fakta er ferdig.
     private VilkårType avklartBetingetVilkårType;

     // I tillegg til betinget vilkår vil det følge tilhørende vilkår
     private List<VilkårType> tilhørendeVilkår;

     static UtledeteVilkår forVilkår(VilkårType betingetVilkårType, List<VilkårType> tilhørendeVilkårTyper) {
         var utledeteVilkår = new UtledeteVilkår();
         utledeteVilkår.avklartBetingetVilkårType = betingetVilkårType;
         utledeteVilkår.tilhørendeVilkår = tilhørendeVilkårTyper;

         return utledeteVilkår;
     }

     public List<VilkårType> getAlleAvklarte() {
         List<VilkårType> avklarteVilkår = new ArrayList<>();
         // Betinget vilkår kan være uavklart inntil aksjonspunkt er løst.
         if (avklartBetingetVilkårType != null) {
             avklarteVilkår.add(avklartBetingetVilkårType);
         }
         avklarteVilkår.addAll(tilhørendeVilkår);
         return avklarteVilkår;
     }
 }
