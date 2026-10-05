package no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AktivitetspengerPerioderResponse(
    @NotNull List<AktivitetspengerPeriode> perioder
) {
}
