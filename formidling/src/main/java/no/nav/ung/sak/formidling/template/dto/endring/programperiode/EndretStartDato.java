package no.nav.ung.sak.formidling.template.dto.endring.programperiode;

import java.time.LocalDate;

public record EndretStartDato(
    LocalDate endretTil,
    LocalDate endretFra
) {
}
