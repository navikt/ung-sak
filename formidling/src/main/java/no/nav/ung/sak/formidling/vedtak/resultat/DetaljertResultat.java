package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.math.BigDecimal;
import java.util.Set;

public record DetaljertResultat(
    Set<BehandlingÅrsakType> behandlingsårsaker,
    Set<DetaljertVilkårResultat> avslåtteVilkår,
    Set<DetaljertVilkårResultat> ikkeVurderteVilkår,
    TilkjentYtelseVerdi tilkjentYtelse,
    boolean tilVurdering
) {

    public boolean harPositivUtbetaling() {
        return tilkjentYtelse != null && tilkjentYtelse.utbetalingsgrad().compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean harÅrsak(BehandlingÅrsakType årsak) {
        return behandlingsårsaker.contains(årsak);
    }

}
