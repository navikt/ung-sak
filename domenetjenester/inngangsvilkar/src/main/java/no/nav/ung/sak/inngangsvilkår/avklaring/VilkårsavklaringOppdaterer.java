package no.nav.ung.sak.inngangsvilkår.avklaring;

import jakarta.enterprise.inject.Instance;

import java.util.Comparator;
import java.util.List;

public interface VilkårsavklaringOppdaterer {

    void settAlleAvklaringerTilFerdig(long behandlingId);

    void settVilkårsperioderTilIkkeVurdertForVilkårsavklaringerUnderArbeid(long behandlingId);

    static List<VilkårsavklaringOppdaterer> sortert(Instance<VilkårsavklaringOppdaterer> oppdaterere) {
        return oppdaterere.stream().sorted(Comparator.comparing(it -> it.getClass().getName())).toList();
    }
}
