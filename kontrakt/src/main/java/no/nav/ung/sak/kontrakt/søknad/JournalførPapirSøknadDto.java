package no.nav.ung.sak.kontrakt.søknad;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import no.nav.k9.felles.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.ung.abac.AppAbacAttributt;
import no.nav.ung.abac.AppAbacAttributtType;
import no.nav.ung.abac.StandardAbacAttributt;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.DokumentId;
import no.nav.ung.sak.typer.JournalpostId;

import java.time.LocalDate;

public record JournalførPapirSøknadDto(

    @JsonProperty(value = "journalpostId", required = true)
    @NotNull
    @Valid
    JournalpostId journalpostId,

    @JsonProperty(value ="aktørId", required = true)
    @NotNull
    @Valid
    AktørId aktørId,

    @JsonProperty(value ="startDato", required = true)
    @NotNull
    @Valid
    LocalDate startDato

) {
}
