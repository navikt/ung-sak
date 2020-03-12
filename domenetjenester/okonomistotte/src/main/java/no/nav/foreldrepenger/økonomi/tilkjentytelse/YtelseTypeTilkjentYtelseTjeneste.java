package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;

public interface YtelseTypeTilkjentYtelseTjeneste {

    List<TilkjentYtelsePeriodeV1> hentTilkjentYtelsePerioder(Long behandlingId);

    boolean erOpphør(BehandlingReferanse ref);

    Boolean erOpphørEtterSkjæringstidspunkt(BehandlingReferanse ref);

    LocalDate hentEndringstidspunkt(Long behandlingId);

}
