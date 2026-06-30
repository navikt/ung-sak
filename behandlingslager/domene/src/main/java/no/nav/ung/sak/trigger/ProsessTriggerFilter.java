package no.nav.ung.sak.trigger;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ProsessTriggerFilter {

    private static final Set<BehandlingÅrsakType> OVERSTYRER_VARSEL_OPPHØR_VED_MAKSDATO = Set.of(
        BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM,
        BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM
    );

    private ProsessTriggerFilter() {
    }

    /**
     * Returnerer true dersom varsel om opphør ved maksdato er overstyrt av en annen årsak
     * (forlenget periode eller manuelt opphør). I slike tilfeller skal det ikke sendes varsel om opphør ved maksdato.
     */
    public static boolean erVarselOpphørVedMaksdatoOverstyrt(Collection<BehandlingÅrsakType> årsaker) {
        return årsaker.stream().anyMatch(OVERSTYRER_VARSEL_OPPHØR_VED_MAKSDATO::contains);
    }

    public static List<Trigger> forKravperioder(List<Trigger> triggere) {
        boolean varselOpphørVedMaksdatoErOverstyrt = triggere.stream()
            .map(Trigger::getÅrsak)
            .anyMatch(OVERSTYRER_VARSEL_OPPHØR_VED_MAKSDATO::contains);

        if (!varselOpphørVedMaksdatoErOverstyrt) {
            return triggere;
        }

        return triggere.stream()
            .filter(it -> it.getÅrsak() != BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO)
            .toList();
    }
}
