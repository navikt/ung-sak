package no.nav.ung.sak.behandlingslager.aktør;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.ung.sak.typer.PersonIdent;

public class FødtBarnInfo {
    public static final String UTEN_NAVN = "UTEN NAVN";
    private PersonIdent ident;
    private String navn;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;

    private FødtBarnInfo(PersonIdent ident, String navn, LocalDate fødselsdato, LocalDate dødsdato) {
        this.ident = ident;
        this.navn = navn;
        this.fødselsdato = fødselsdato;
        this.dødsdato = dødsdato;
    }

    public PersonIdent getIdent() {
        return ident;
    }

    public String getNavn() {
        return navn == null ? UTEN_NAVN : navn;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    public Optional<LocalDate> getDødsdato() {
        return Optional.ofNullable(dødsdato);
    }

    public static class Builder {
        private PersonIdent ident;
        private String navn;
        private LocalDate fødselsdato;
        private LocalDate dødsdato;

        public Builder medIdent(PersonIdent ident) {
            this.ident = ident;
            return this;
        }

        public Builder medNavn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            this.dødsdato = dødsdato;
            return this;
        }

        public FødtBarnInfo build() {
            return new FødtBarnInfo(ident, navn, fødselsdato, dødsdato);
        }
    }
}
