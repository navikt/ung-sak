package no.nav.k9.sak.domene.uttak.input;

import java.time.LocalDate;

import no.nav.k9.sak.typer.AktørId;

public class UttakPersonInfo {

    private LocalDate fødselsdato;
    private LocalDate dødsdato;
    private AktørId aktørId;

    public UttakPersonInfo(AktørId aktørId, LocalDate fødselsdato, LocalDate dødsdato) {
        if (aktørId == null && fødselsdato == null) {
            throw new IllegalArgumentException("Kan ikke identifisere person dersom både aktørId og fødselsdato begge er null");
        }
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
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
