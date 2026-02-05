package no.nav.ung.ytelse.ungdomsprogramytelsen.papirsøknad;

import java.time.LocalDate;

public record PapirsøknadDto(String deltakerNavn, String deltakerIdent, LocalDate startdato, LocalDate søknadsdato) {
}
