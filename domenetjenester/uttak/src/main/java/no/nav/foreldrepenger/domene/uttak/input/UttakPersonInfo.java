package no.nav.foreldrepenger.domene.uttak.input;

import java.time.LocalDate;

import no.nav.k9.sak.typer.AktørId;

public class UttakPersonInfo {

    private LocalDate fødselsdato;
    private LocalDate dødsdato;
    private AktørId aktørId;

    public UttakPersonInfo(AktørId aktørId, LocalDate fødselsdato, LocalDate dødsdato) {
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
        this.aktørId = aktørId;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public AktørId getAktørId() {
        return aktørId;
    }
}
