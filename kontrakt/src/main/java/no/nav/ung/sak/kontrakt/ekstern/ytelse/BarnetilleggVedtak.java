package no.nav.ung.sak.kontrakt.ekstern.ytelse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Barnetillegg i gjeldende vedtak i en sak.
 *
 * @param perioder perioder der bruker har rett på barnetillegg. Like perioder som følger etter hverandre er slått sammen.
 */
public record BarnetilleggVedtak(
    @NotNull YtelseType ytelse,
    @NotNull String saksnummer,
    @NotNull UUID vedtakReferanse,
    @NotNull LocalDateTime vedtattTidspunkt,
    @NotNull List<@Valid BarnetilleggPeriode> perioder
) {
}
