package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.endring.programperiode;

import java.time.LocalDate;

public record EndretSluttDato(
    LocalDate endretTil,
    LocalDate endretFra,
    LocalDate opphørStartDato,
    LocalDate sisteUtbetalingsdato) {
}
