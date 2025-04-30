package no.nav.ung.sak.test.util.behandling.personopplysning;

import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;


public final class Personopplysning {

    private AktørId aktørId;
    private NavBrukerKjønn brukerKjønn = NavBrukerKjønn.UDEFINERT;
    private String navn;
    private LocalDate dødsdato;
    private LocalDate fødselsdato;

    public AktørId getAktørId() {
        return aktørId;
    }

    public NavBrukerKjønn getBrukerKjønn() {
        return brukerKjønn;
    }

    public String getNavn() {
        return navn;
    }

    public LocalDate getDødsdato() {
        return dødsdato;
    }

    public LocalDate getFødselsdato() {
        return fødselsdato;
    }

    private Personopplysning(Builder builder) {
        this.aktørId = builder.aktørId;
        this.brukerKjønn = builder.brukerKjønn;
        this.navn = builder.navn;
        this.dødsdato = builder.dødsdato;
        this.fødselsdato = builder.fødselsdato;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Bare overstyr enkelt verdier hvis du trenger det
     */
    public static Builder builderMedDefaultVerdier(AktørId aktørId) {
        return Personopplysning.builder()
            .brukerKjønn(NavBrukerKjønn.KVINNE)
            .fødselsdato(LocalDate.now().minusYears(25))
            .navn("Foreldre")
            .aktørId(aktørId);
    }

    public static final class Builder {
        private AktørId aktørId;
        private NavBrukerKjønn brukerKjønn;
        private String navn;
        private LocalDate dødsdato;
        private LocalDate fødselsdato;

        private Builder() {
        }

        public Personopplysning build() {
            return new Personopplysning(this);
        }

        public Builder aktørId(AktørId aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder brukerKjønn(NavBrukerKjønn brukerKjønn) {
            this.brukerKjønn = brukerKjønn;
            return this;
        }

        public Builder navn(String navn) {
            this.navn = navn;
            return this;
        }

        public Builder dødsdato(LocalDate dødsdato) {
            this.dødsdato = dødsdato;
            return this;
        }

        public Builder fødselsdato(LocalDate fødselsdato) {
            this.fødselsdato = fødselsdato;
            return this;
        }
    }
}
