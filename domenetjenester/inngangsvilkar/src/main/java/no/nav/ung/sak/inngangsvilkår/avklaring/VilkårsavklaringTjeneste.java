package no.nav.ung.sak.inngangsvilkår.avklaring;

import jakarta.enterprise.inject.Instance;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

public interface VilkårsavklaringTjeneste {

    void settAlleAvklaringerTilFerdig(long behandlingId);

    void settVilkårsperioderTilIkkeVurdertForVilkårsavklaringerUnderArbeid(long behandlingId);


    /**
     * Angir om denne tjenesten gjelder for gitt {@link BehandlingÅrsakType}.
     * <p>
     * Vilkårsavklaringer styres av behandlingstriggere representert som {@link BehandlingÅrsakType}.
     * For hvert vilkår skal det finnes en entydig kobling til én årsak.
     * Brukes for å avgjøre hvilken {@code AvklaringTjeneste} (implementasjon av
     * {@link VilkårsavklaringTjeneste}) som skal håndtere en gitt trigger.
     */
    boolean gjelderFor(BehandlingÅrsakType behandlingÅrsakType);

    Optional<VilkårsavklaringUnderArbeid> hentSenesteAvklaringUnderArbeid(long behandlingId);

    static List<VilkårsavklaringTjeneste> sortert(Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester) {
        return vilkårsavklaringTjenester.stream().sorted(Comparator.comparing(it -> it.getClass().getName())).toList();
    }

    static Optional<VilkårsavklaringTjeneste> finnForÅrsak(Instance<VilkårsavklaringTjeneste> vilkårsavklaringTjenester, BehandlingÅrsakType behandlingÅrsakType) {
        var treff = vilkårsavklaringTjenester.stream().filter(it -> it.gjelderFor(behandlingÅrsakType)).toList();
        if (treff.size() > 1) {
            throw new IllegalStateException("Har flere " + VilkårsavklaringTjeneste.class.getSimpleName() + " som gjelder for behandlingårsak=" + behandlingÅrsakType + ": " + treff);
        }
        return treff.stream().findFirst();
    }
}

