package no.nav.k9.sak.inngangsvilkår.omsorg.regelmodell;

import java.time.LocalDate;

public class Fosterbarn {
    private final String aktørId;
    private final LocalDate fødselsdato;
    private final LocalDate dødsdato;

    public Fosterbarn(String aktørId, LocalDate fødselsdato, LocalDate dødsdato) {
        this.aktørId = aktørId;
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
    }

    public String getAktørId() {
        return aktørId;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    @Override
    public String toString() {
        return "Fosterbarn{" +
            "aktørId='" + aktørId + '\'' +
            ", fødselsdato=" + fødselsdato +
            ", dødsdato=" + dødsdato +
            '}';
    }
}
