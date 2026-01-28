package no.nav.ung.sak.kontrakt.søknad;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.sak.felles.abac.StandardAbacAttributt;
import no.nav.ung.sak.felles.typer.JournalpostId;

public record HentPapirSøknadRequestDto(

    @StandardAbacAttributt(StandardAbacAttributtType.JOURNALPOST_ID)
    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Valid
    JournalpostId journalpostId,


    @StandardAbacAttributt(StandardAbacAttributtType.FNR)
    @JsonProperty(value = "deltakerIdent", required = true)
    @NotNull
    @Size(max = 20)
    @Pattern(regexp = "^\\d+$", message = "ident [${validatedValue}] matcher ikke tillatt pattern [{regexp}]")
    @Valid
    String deltakerIdent
) {
}
