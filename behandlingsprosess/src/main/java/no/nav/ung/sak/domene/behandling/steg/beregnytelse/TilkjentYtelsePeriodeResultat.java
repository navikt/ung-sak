package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.util.Map;

public record TilkjentYtelsePeriodeResultat(TilkjentYtelseVerdi verdi, Map<String, String> sporing) {
}
