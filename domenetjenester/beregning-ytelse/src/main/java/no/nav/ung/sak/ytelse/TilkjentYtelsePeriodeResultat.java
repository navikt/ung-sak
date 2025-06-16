package no.nav.ung.sak.ytelse;

import java.util.Map;

import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

public record TilkjentYtelsePeriodeResultat(TilkjentYtelseVerdi verdi, Map<String, String> sporing) {
}
