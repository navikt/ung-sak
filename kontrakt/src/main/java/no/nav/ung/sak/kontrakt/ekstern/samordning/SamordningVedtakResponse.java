package no.nav.ung.sak.kontrakt.ekstern.samordning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SamordningVedtakResponse(
    @NotNull List<@Valid SamordningVedtak> vedtak
) {
}
