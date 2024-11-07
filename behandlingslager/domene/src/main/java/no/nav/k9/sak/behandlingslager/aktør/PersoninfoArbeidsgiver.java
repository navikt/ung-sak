package no.nav.k9.sak.behandlingslager.aktør;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Objects;

import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

public class PersoninfoArbeidsgiver {

    private AktørId aktørId;
    private String navn;
    private PersonIdent personIdent;
    private LocalDate fødselsdato;

    private PersoninfoArbeidsgiver(AktørId aktørId, String navn, PersonIdent personIdent, LocalDate fødselsdato) {
        this.aktørId = requireNonNull(aktørId, "aktørId er påkrevd, men var null");
        this.navn = requireNonNull(navn, "navn er påkrevd, men var null");
        this.personIdent = requireNonNull(personIdent, "personIdent er påkrevd, men var null");
        this.fødselsdato = requireNonNull(fødselsdato, "fødselsdato er påkrevd, men var null");
    }

    public AktørId getAktørId() {
        return aktørId;
    }

    public PersonIdent getPersonIdent() {
        return personIdent;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersoninfoArbeidsgiver that = (PersoninfoArbeidsgiver) o;
        return aktørId.equals(that.aktørId) &&
            Objects.equals(navn, that.navn) &&
            Objects.equals(personIdent, that.personIdent) &&
            Objects.equals(fødselsdato, that.fødselsdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(aktørId);
    }

    @Override
    public String toString() {
        return "PersoninfoArbeidsgiver{" +
            "aktørId=" + aktørId +
            ", navn='" + navn + '\'' +
            ", personIdent=" + personIdent +
            ", fødselsdato=" + fødselsdato +
            '}';
    }

    public static class Builder {
        private AktørId aktørId;
        private String navn;
        private PersonIdent personIdent;
        private LocalDate fødselsdato;

        public Builder medAktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder medNavn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder medPersonIdent(PersonIdent personIdent) {
            this.personIdent = personIdent;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }

        public PersoninfoArbeidsgiver bygg() {
            return new PersoninfoArbeidsgiver(aktørId, navn, personIdent, fødselsdato);
        }
    }
}
