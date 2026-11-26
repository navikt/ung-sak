package no.nav.ung.sak.kontrakt.ekstern.ytelse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Gjeldende vedtak i en sak.
 *
 * @param perioder perioder der bruker har rett på ytelsen. Gjelder også perioder med 0 kr utbetalt.
 */
public record Ytelse(
    @NotNull YtelseType ytelse,
    @NotNull String saksnummer,
    @NotNull UUID vedtakReferanse,
    @NotNull LocalDateTime vedtattTidspunkt,
    @NotNull List<@Valid YtelsePeriode> perioder
) {
}
