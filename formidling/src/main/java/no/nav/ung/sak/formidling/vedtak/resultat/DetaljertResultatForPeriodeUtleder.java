package no.nav.ung.sak.formidling.vedtak.resultat;

import no.nav.fpsak.tidsserie.LocalDateInterval;
import no.nav.ung.kodeverk.behandling.BehandlingÅrsakType;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;

public interface DetaljertResultatForPeriodeUtleder {

    Set<DetaljertResultatInfo> bestemDetaljertResultat(
        LocalDateInterval periode,
        SamletVilkårResultatOgBehandlingÅrsaker vilkårResultat,
        TilkjentYtelseVerdi tilkjentYtelse);

    //*** Felles sjekk ***

    static DetaljertResultatInfo nyPeriodeDetaljertResultat(Set<DetaljertVilkårResultat> avslåtteVilkår, TilkjentYtelseVerdi tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            if (!avslåtteVilkår.isEmpty()) {
                return DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR, "Avslått inngangsvilkår for ny periode");
            }

            return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_KUN_VILKÅR);
        }

        return DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_UTBETALING);
    }

    static DetaljertResultatInfo kontrollerInntektDetaljertResultat(TilkjentYtelseVerdi tilkjentYtelse) {
        if (tilkjentYtelse == null) {
            // Usikker om dette er mulig
            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_UTEN_TILKJENT_YTELSE);
        }
        if (tilkjentYtelse.utbetalingsgrad().compareTo(BigDecimal.valueOf(100)) >= 0) {
            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_FULL_UTBETALING);
        }

        if (tilkjentYtelse.utbetalingsgrad().compareTo(BigDecimal.ZERO) <= 0) {
            return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_INGEN_UTBETALING);
        }

        return DetaljertResultatInfo.of(DetaljertResultatType.KONTROLLER_INNTEKT_REDUKSJON);
    }

    static Optional<DetaljertResultatInfo> håndterUkjenteTilfeller(TilkjentYtelseVerdi tilkjentYtelse, Set<DetaljertVilkårResultat> avslåtteVilkår, Set<BehandlingÅrsakType> relevanteÅrsaker) {
        if (!avslåtteVilkår.isEmpty()) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_INNGANGSVILKÅR, "Avslåtte inngangsvilkår ukjent årsak"));
        }

        if (tilkjentYtelse == null) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.INNVILGELSE_ANNET, "Innvilgede vilkår uten tilkjent ytelse"));
        }

        if (tilkjentYtelse.utbetalingsgrad().compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.AVSLAG_ANNET, "Innvilgede vilkår 0 kr tilkjent ytelse"));
        }

        if (relevanteÅrsaker.isEmpty()) {
            return Optional.of(DetaljertResultatInfo.of(DetaljertResultatType.INNVILGET_UTEN_ÅRSAK, "Innvilget uten årsak"));
        }

        return Optional.empty();
    }



}
