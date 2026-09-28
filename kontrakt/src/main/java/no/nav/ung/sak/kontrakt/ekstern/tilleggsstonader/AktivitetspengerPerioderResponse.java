package no.nav.ung.sak.kontrakt.ekstern.tilleggsstonader;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Innvilgede perioder med aktivitetspenger som overlapper forespurt periode. Periodene kuttes ikke ved fom/tom i forespørselen.
 */
public record AktivitetspengerPerioderResponse(
    @JsonProperty(value = "perioder", required = true)
    @NotNull
    @Valid
    List<AktivitetspengerPeriode> perioder
) {
}
