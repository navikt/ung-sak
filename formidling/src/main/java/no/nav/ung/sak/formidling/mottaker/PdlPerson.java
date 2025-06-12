package no.nav.ung.sak.formidling.mottaker;

import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;

public record PdlPerson(
    String fnr,
    AktørId aktørId,
    String navn,
    LocalDate dødsdato) {}
