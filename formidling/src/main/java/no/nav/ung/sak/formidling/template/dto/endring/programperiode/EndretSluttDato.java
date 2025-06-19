package no.nav.ung.sak.formidling.template.dto.endring.programperiode;

import java.time.LocalDate;

public record EndretSluttDato(
    LocalDate endretTil,
    LocalDate endretFra,
    LocalDate opphørStartDato,
    boolean harFlyttetBakover,
    LocalDate sisteUtbetalingsdato) {
}
