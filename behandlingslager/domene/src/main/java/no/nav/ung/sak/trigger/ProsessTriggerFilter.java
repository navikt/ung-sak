package no.nav.ung.sak.trigger;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ProsessTriggerFilter {

    private static final Set<BehandlingÅrsakType> OVERSTYRER_VARSEL_OPPHØR_VED_MAKSDATO = Set.of(
        BehandlingÅrsakType.RE_HENDELSE_FORLENGET_PERIODE_UNGDOMSPROGRAM,
        BehandlingÅrsakType.RE_HENDELSE_OPPHØR_UNGDOMSPROGRAM,
        BehandlingÅrsakType.RE_HENDELSE_OPPHØR_OPPHEVET_UNGDOMSPROGRAM
    );

    private ProsessTriggerFilter() {
    }

    /**
     * Returnerer true dersom behandlingen er overstyrt av forlenget periode, manuelt opphør, eller opphevelse av opphør.
     * Disse hendelsene endrer selve periode-/maksdato-grunnlaget (utvider maksdato, setter tom < maksdato, eller
     * gjenåpner en tidligere avsluttet periode), og kan derfor legitimt gjøre at varsel om opphør ved maksdato
     * ikke lenger er relevant for akkurat denne behandlingen eller skal avbrytes.
     * Andre tilleggsårsaker (f.eks. inntektskontroll) endrer ikke dette grunnlaget, og overstyrer derfor ikke varselet.
     */
    public static boolean erOverstyrtAvAnnenHendelse(Collection<BehandlingÅrsakType> årsaker) {
        return årsaker.stream().anyMatch(OVERSTYRER_VARSEL_OPPHØR_VED_MAKSDATO::contains);
    }

    public static List<Trigger> forKravperioder(List<Trigger> triggere) {
        boolean varselOpphørVedMaksdatoErOverstyrt = erOverstyrtAvAnnenHendelse(
            triggere.stream().map(Trigger::getÅrsak).toList());

        if (!varselOpphørVedMaksdatoErOverstyrt) {
            return triggere;
        }

        return triggere.stream()
            .filter(it -> it.getÅrsak() != BehandlingÅrsakType.RE_VARSEL_OPPHOR_VED_MAKSDATO)
            .toList();
    }
}
