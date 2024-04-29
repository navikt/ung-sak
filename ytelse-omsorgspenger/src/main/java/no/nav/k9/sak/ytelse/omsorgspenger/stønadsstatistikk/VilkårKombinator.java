package no.nav.k9.sak.ytelse.omsorgspenger.stønadsstatistikk;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import no.nav.k9.kodeverk.vilkår.Utfall;

public class VilkårKombinator {

    public static <T> Map<T, VilkårUtfall> kombinerVilkår(Map<T, VilkårUtfall> lhs, Map<T, VilkårUtfall> rhs) {
        if (lhs == null && rhs == null) {
            return null;
        }
        if (rhs == null) {
            return new LinkedHashMap<>(lhs);
        }
        if (lhs == null) {
            return new LinkedHashMap<>(rhs);
        }
        Map<T, VilkårUtfall> kombinerteVilkår = new LinkedHashMap<>(lhs);

        for (Map.Entry<T, VilkårUtfall> e : rhs.entrySet()) {
            VilkårUtfall ny = e.getValue();
            if (kombinerteVilkår.containsKey(e.getKey())) {
                VilkårUtfall forrige = kombinerteVilkår.get(e.getKey());
                Utfall hovedutfall = forrige.getUtfall() == Utfall.OPPFYLT ? forrige.getUtfall() : ny.getUtfall();
                if (forrige.getDetaljer() != null || ny.getDetaljer() != null) {
                    Set<DetaljertVilkårUtfall> detaljer = new LinkedHashSet<>();
                    detaljer.addAll(forrige.getDetaljer() != null ? forrige.getDetaljer() : Set.of());
                    detaljer.addAll(ny.getDetaljer() != null ? ny.getDetaljer() : Set.of());
                    kombinerteVilkår.put(e.getKey(), new VilkårUtfall(hovedutfall, detaljer));
                } else {
                    kombinerteVilkår.put(e.getKey(), new VilkårUtfall(hovedutfall));
                }
            } else {
                kombinerteVilkår.put(e.getKey(), ny);
            }
        }
        return kombinerteVilkår;
    }
}
