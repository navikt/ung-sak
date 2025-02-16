package no.nav.ung.sak.domene.behandling.steg.beregnytelse;

import no.nav.fpsak.tidsserie.LocalDateTimeline;
import no.nav.ung.sak.behandlingslager.tilkjentytelse.TilkjentYtelseVerdi;

import java.util.Map;

public record TilkjentYtelseResultat(LocalDateTimeline<TilkjentYtelseVerdi> tilkjentYtelseTidslinje, Map<String, String> sporing) {
}
