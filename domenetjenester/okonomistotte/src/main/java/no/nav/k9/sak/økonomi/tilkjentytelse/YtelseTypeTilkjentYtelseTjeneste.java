package no.nav.k9.sak.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.util.List;

import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;
import no.nav.k9.sak.behandling.BehandlingReferanse;

public interface YtelseTypeTilkjentYtelseTjeneste {

    List<TilkjentYtelsePeriodeV1> hentTilkjentYtelsePerioder(Long behandlingId);

    boolean erOpphør(BehandlingReferanse ref);

    Boolean erOpphørEtterSkjæringstidspunkt(BehandlingReferanse ref);

    LocalDate hentEndringstidspunkt(Long behandlingId);

}
