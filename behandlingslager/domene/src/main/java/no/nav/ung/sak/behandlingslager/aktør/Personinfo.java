package no.nav.ung.sak.behandlingslager.aktør;

import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import static java.util.Objects.requireNonNull;

public class Personinfo {

    private AktørId aktørId;
    private String navn;
    private PersonIdent personIdent;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;
    private Set<Familierelasjon> familierelasjoner = Collections.emptySet();
    private String diskresjonskode;
    private Språkkode foretrukketSpråk;

    private Personinfo() {
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

    public int getAlder(LocalDate dato) {
        return (int) ChronoUnit.YEARS.between(fødselsdato, dato);
    }

    public int getAlderIDag() {
        return getAlder(LocalDate.now());
    }

    public Set<Familierelasjon> getFamilierelasjoner() {
        return Collections.unmodifiableSet(familierelasjoner);
    }


    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public Språkkode getForetrukketSpråk() {
        return foretrukketSpråk;
    }

    public String getDiskresjonskode() {
        return diskresjonskode;
    }

    @Override
    public String toString() {
        // tar ikke med aktørId/fnr/personident i toString, så det ikke lekker i logger etc.
        return getClass().getSimpleName() + "<fødselsdato=" + fødselsdato + ">"; //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static class Builder {
        private Personinfo personinfoMal;

        public Builder() {
            personinfoMal = new Personinfo();
        }

        public Builder medAktørId(AktørId aktørId) {
            personinfoMal.aktørId = aktørId;
            return this;
        }

        public Builder medNavn(String navn) {
            personinfoMal.navn = navn;
            return this;
        }

        /**
         * @deprecated Bruk {@link #medPersonIdent(PersonIdent)} i stedet!
         */
        @Deprecated
        public Builder medFnr(String fnr) {
            personinfoMal.personIdent = PersonIdent.fra(fnr);
            return this;
        }

        public Builder medPersonIdent(PersonIdent fnr) {
            personinfoMal.personIdent = fnr;
            return this;
        }

        public Builder medFødselsdato(LocalDate fødselsdato) {
            personinfoMal.fødselsdato = fødselsdato;
            return this;
        }

        public Builder medDødsdato(LocalDate dødsdato) {
            personinfoMal.dødsdato = dødsdato;
            return this;
        }

        public Builder medFamilierelasjon(Set<Familierelasjon> familierelasjon) {
            personinfoMal.familierelasjoner = familierelasjon;
            return this;
        }

        public Builder medDiskresjonsKode(String diskresjonsKode) {
            personinfoMal.diskresjonskode = diskresjonsKode;
            return this;
        }

        public Builder medForetrukketSpråk(Språkkode språk) {
            personinfoMal.foretrukketSpråk = språk;
            return this;
        }

        public Personinfo build() {
            requireNonNull(personinfoMal.aktørId, "Navbruker må ha aktørId"); //$NON-NLS-1$
            requireNonNull(personinfoMal.personIdent, "Navbruker må ha fødselsnummer"); //$NON-NLS-1$
            requireNonNull(personinfoMal.navn, "Navbruker må ha navn"); //$NON-NLS-1$
            requireNonNull(personinfoMal.fødselsdato, "Navbruker må ha fødselsdato"); //$NON-NLS-1$
            return personinfoMal;
        }

    }

}
