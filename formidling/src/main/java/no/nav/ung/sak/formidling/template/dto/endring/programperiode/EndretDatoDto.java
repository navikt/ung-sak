package no.nav.ung.sak.formidling.template.dto.endring.programperiode;

import java.time.LocalDate;

public record EndretDatoDto(
    LocalDate nyDato,
    LocalDate tidligereDato
) {
}
