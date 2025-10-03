package no.nav.ung.sak.kontrakt.søknad;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;
import no.nav.ung.sak.typer.JournalpostId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;

public record JournalførPapirSøknadDto(

    @StandardAbacAttributt(StandardAbacAttributtType.JOURNALPOST_ID)
    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Valid
    JournalpostId journalpostId,

    @StandardAbacAttributt(StandardAbacAttributtType.FNR)
    @JsonProperty(value ="personIdent", required = true)
    @NotNull
    @Valid
    PersonIdent personIdent,

    @JsonProperty(value ="startDato", required = true)
    @NotNull
    @Valid
    LocalDate startDato

) {
}
