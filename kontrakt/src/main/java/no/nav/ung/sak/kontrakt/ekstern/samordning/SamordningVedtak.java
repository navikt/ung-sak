package no.nav.ung.sak.kontrakt.ekstern.samordning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Gjeldende vedtak i en sak.
 *
 * @param perioder perioder der bruker har rett på ytelsen, og derfor ikke kan ha andre livsoppholdsytelser. Gjelder også
 *                 perioder med 0 kr utbetalt.
 */
public record SamordningVedtak(
    @NotNull SamordningYtelse ytelse,
    @NotNull String saksnummer,
    @NotNull UUID vedtakReferanse,
    @NotNull LocalDateTime vedtattTidspunkt,
    @NotNull List<@Valid SamordningPeriode> perioder
) {
}
