package no.nav.foreldrepenger.økonomi.tilkjentytelse;

import java.time.LocalDate;
import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.k9.oppdrag.kontrakt.tilkjentytelse.TilkjentYtelsePeriodeV1;

public interface YtelseTypeTilkjentYtelseTjeneste {

    List<TilkjentYtelsePeriodeV1> hentTilkjentYtelsePerioder(Long behandlingId);

    boolean erOpphør(Behandlingsresultat behandlingsresultat);

    Boolean erOpphørEtterSkjæringstidspunkt(Behandling behandling, Behandlingsresultat behandlingsresultat);

    LocalDate hentEndringstidspunkt(Long behandlingId);
}
