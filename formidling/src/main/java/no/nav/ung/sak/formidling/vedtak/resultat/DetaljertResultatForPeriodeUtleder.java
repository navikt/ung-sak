package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.util.Set;

public interface DetaljertResultatForPeriodeUtleder {
    Set<DetaljertResultatInfo> bestemDetaljertResultat(
        LocalDateInterval periode,
        SamletVilkårResultatOgBehandlingÅrsaker vilkårResultat,
        TilkjentYtelseVerdi tilkjentYtelse);
}
