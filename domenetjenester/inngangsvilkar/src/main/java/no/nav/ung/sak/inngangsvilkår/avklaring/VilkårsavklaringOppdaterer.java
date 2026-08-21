package no.nav.ung.sak.inngangsvilkår.avklaring;

import jakarta.enterprise.inject.Instance;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

public interface VilkårsavklaringOppdaterer {

    void settAlleAvklaringerTilFerdig(long behandlingId);

    void settVilkårsperioderTilIkkeVurdertForVilkårsavklaringerUnderArbeid(long behandlingId);

    boolean gjelderFor(BehandlingÅrsakType behandlingÅrsakType);

    /**
     * @return den seneste avklaringen (avklaringtype + periode) som fortsatt er under arbeid, hvis noen finnes.
     */
    Optional<VilkårsavklaringUnderArbeid> hentSenesteAvklaringUnderArbeid(long behandlingId);

    static List<VilkårsavklaringOppdaterer> sortert(Instance<VilkårsavklaringOppdaterer> oppdaterere) {
        return oppdaterere.stream().sorted(Comparator.comparing(it -> it.getClass().getName())).toList();
    }

    static Optional<VilkårsavklaringOppdaterer> finnForÅrsak(Instance<VilkårsavklaringOppdaterer> oppdaterere, BehandlingÅrsakType behandlingÅrsakType) {
        var treff = oppdaterere.stream().filter(it -> it.gjelderFor(behandlingÅrsakType)).toList();
        if (treff.size() > 1) {
            throw new IllegalStateException("Har flere " + VilkårsavklaringOppdaterer.class.getSimpleName() + " som gjelder for behandlingårsak=" + behandlingÅrsakType + ": " + treff);
        }
        return treff.stream().findFirst();
    }
}

