package no.nav.ung.sak.kontrakt.ekstern.ytelse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BarnetilleggResponse(
    @NotNull List<@Valid BarnetilleggVedtak> vedtak
) {
}
