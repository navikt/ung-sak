package no.nav.ung.sak.kontrakt.søknad;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.sak.abac.StandardAbacAttributt;
import no.nav.ung.sak.typer.JournalpostId;

public record SendInnPapirsøknadopplysningerRequestDto(

    @StandardAbacAttributt(StandardAbacAttributtType.FNR)
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "ident [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    String deltakerIdent,

    @StandardAbacAttributt(StandardAbacAttributtType.JOURNALPOST_ID)
    @Valid
    JournalpostId journalpostIdForPapirsøknad
) {
}
