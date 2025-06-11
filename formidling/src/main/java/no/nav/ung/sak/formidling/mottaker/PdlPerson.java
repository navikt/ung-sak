package no.nav.ung.sak.formidling.mottaker;

import no.nav.ung.sak.typer.AktørId;

public record PdlPerson(String fnr, AktørId aktørId, String navn) {}
