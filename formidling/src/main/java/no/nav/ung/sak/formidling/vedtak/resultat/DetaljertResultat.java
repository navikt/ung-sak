package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;

import java.util.Set;

public record DetaljertResultat(
    Set<BehandlingÅrsakType> behandlingsårsaker,
    Set<DetaljertVilkårResultat> avslåtteVilkår,
    Set<DetaljertVilkårResultat> ikkeVurderteVilkår,
    UtbetalingsgradType utbetalingsgrad,
    boolean tilVurdering
) {

    public boolean harÅrsak(BehandlingÅrsakType årsak) {
        return behandlingsårsaker.contains(årsak);
    }

}
