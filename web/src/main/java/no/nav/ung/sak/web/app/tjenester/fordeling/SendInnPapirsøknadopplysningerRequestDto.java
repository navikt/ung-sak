package no.nav.ung.sak.web.app.tjenester.fordeling;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;
import no.nav.ung.sak.typer.JournalpostId;


import java.time.LocalDate;
import java.util.UUID;

public record SendInnPapirsøknadopplysningerRequestDto(

    @StandardAbacAttributt(StandardAbacAttributtType.FNR)
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "ident [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    String deltakerIdent,

    @StandardAbacAttributt(StandardAbacAttributtType.JOURNALPOST_ID)
    @Valid
    JournalpostId journalpostIdForPapirsøknad,

    UUID deltakelseId,

    LocalDate startdato
) {
}
