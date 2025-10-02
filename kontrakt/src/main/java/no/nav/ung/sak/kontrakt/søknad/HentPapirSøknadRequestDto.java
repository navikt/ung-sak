package no.nav.ung.sak.kontrakt.søknad;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.AppAbacAttributt;
import no.nav.ung.abac.AppAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;
import no.nav.ung.sak.typer.JournalpostId;

public record HentPapirSøknadRequestDto(

    @StandardAbacAttributt(StandardAbacAttributtType.JOURNALPOST_ID)
    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Valid
    JournalpostId journalpostId,

    @AppAbacAttributt(AppAbacAttributtType.DOKUMENT_ID)
    @JsonProperty(value = "dokumentId", required = true)
    @NotBlank
    String dokumentId
) {
}
