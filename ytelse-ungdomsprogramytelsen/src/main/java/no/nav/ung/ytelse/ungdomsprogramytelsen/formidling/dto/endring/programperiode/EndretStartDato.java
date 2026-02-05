package no.nav.ung.ytelse.ungdomsprogramytelsen.formidling.dto.endring.programperiode;

import java.time.LocalDate;

public record EndretStartDato(
    LocalDate endretTil,
    LocalDate endretFra
) {
}
