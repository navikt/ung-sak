package no.nav.ung.sak.kontrakt.ungdomsytelse;

import java.time.LocalDate;

public record UngdomsprogramInformasjonDto(
    LocalDate startdato,
    LocalDate maksdatoForDeltakelse,
    LocalDate opphørsdato,
    Integer antallDagerTidligereUtbetalt,
    boolean harForlengetPeriode
) {
}
