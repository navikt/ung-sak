package no.nav.ung.sak.kontrakt.ungdomsytelse;

import java.time.LocalDate;

public record UngdomsprogramInformasjonDto(
    LocalDate maksdatoForDeltakelse,
    LocalDate opphørsdato,
    Integer antallDagerBruktForTilkjentePerioder
) {
}
