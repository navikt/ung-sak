package no.nav.ung.sak.web.app.tjenester.fordeling;

import java.time.LocalDate;

public record PapirsøknadDto(String deltakerNavn, String deltakerIdent, LocalDate startdato, LocalDate søknadsdato) {
}
