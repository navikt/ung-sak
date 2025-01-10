package no.nav.ung.sak.formidling.domene;

import no.nav.ung.sak.typer.AktørId;

public record PdlPerson(String fnr, AktørId aktørId, String navn, java.time.LocalDate fødselsdato) {}
