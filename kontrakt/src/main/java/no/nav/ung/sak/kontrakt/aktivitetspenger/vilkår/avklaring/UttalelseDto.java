package no.nav.ung.sak.kontrakt.aktivitetspenger.vilkår.avklaring;

import java.time.LocalDateTime;

public record UttalelseDto(
    boolean harUttalelse,
    String uttalelseTekst,
    LocalDateTime mottattTidspunkt
) {
}
