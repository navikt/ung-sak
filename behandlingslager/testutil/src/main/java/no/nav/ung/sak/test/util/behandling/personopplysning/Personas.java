package no.nav.ung.sak.test.util.behandling.personopplysning;

import no.nav.ung.kodeverk.person.NavBrukerKjønn;
import no.nav.ung.kodeverk.person.RelasjonsRolleType;
import no.nav.ung.sak.test.util.behandling.personopplysning.PersonInformasjon.Builder;
import no.nav.ung.sak.typer.AktørId;

import java.time.LocalDate;

public class Personas {
    private Builder builder;
    private AktørId aktørId;
    private Personopplysning.Builder persInfoBuilder;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;

    public Personas(Builder builder) {
        this.builder = builder;
        this.persInfoBuilder = Personopplysning.builder();
    }

    private Personas voksenPerson(AktørId aktørId, NavBrukerKjønn kjønn) {
        if (this.aktørId == null) {
            this.aktørId = aktørId;
            this.persInfoBuilder = Personopplysning.builder();
            this.fødselsdato = LocalDate.now().minusYears(30);  // VOKSEN
        } else {
            throw new IllegalArgumentException("En Personas har kun en aktørId, allerede satt til " + this.aktørId + ", angitt=" + aktørId);
        }
        builder.leggTilPersonopplysninger(persInfoBuilder
            .aktørId(aktørId)
            .brukerKjønn(kjønn)
            .fødselsdato(fødselsdato));
        return this;
    }


    public Personas ungdom(AktørId aktørId, LocalDate fødselsdato) {
        return ungdom(aktørId, fødselsdato, "Test Testesen", null);
    }

    public Personas ungdom(AktørId aktørId, LocalDate fødselsdato, String navn, LocalDate dødsdato) {
        if (this.aktørId == null) {
            this.aktørId = aktørId;
            this.fødselsdato = fødselsdato;
        } else {
            throw new IllegalArgumentException("En Personas har kun en aktørId, allerede satt til " + this.aktørId + ", angitt=" + aktørId);
        }
        builder.leggTilPersonopplysninger(persInfoBuilder
            .aktørId(aktørId)
            .brukerKjønn(NavBrukerKjønn.MANN)
            .fødselsdato(fødselsdato)
            .dødsdato(dødsdato)
            .navn(navn));

        return this;
    }

    public Personas barn(AktørId aktørId, LocalDate fødselsdato) {
        if (this.aktørId == null) {
            this.aktørId = aktørId;
            this.fødselsdato = fødselsdato;
        } else {
            throw new IllegalArgumentException("En Personas har kun en aktørId, allerede satt til " + this.aktørId + ", angitt=" + aktørId);
        }
        builder.leggTilPersonopplysninger(persInfoBuilder
            .aktørId(aktørId)
            .fødselsdato(fødselsdato)
            .brukerKjønn(NavBrukerKjønn.UDEFINERT)
        );

        return this;
    }

    public Personas dødsdato(LocalDate dødsdato) {
        this.persInfoBuilder.dødsdato(dødsdato);
        return this;
    }

    public Personas kvinne(AktørId aktørId) {
        voksenPerson(aktørId, NavBrukerKjønn.KVINNE);
        return this;
    }

    public Personas mann(AktørId aktørId) {
        voksenPerson(aktørId, NavBrukerKjønn.MANN);
        return this;
    }

    public PersonInformasjon build() {
        return builder.build();
    }

    public Personas relasjonTil(AktørId tilAktørId, RelasjonsRolleType rolle) {
        builder.leggTilRelasjon(PersonRelasjon.builder()
            .fraAktørId(aktørId)
            .tilAktørId(tilAktørId)
            .relasjonsrolle(rolle));
        return this;
    }

}
