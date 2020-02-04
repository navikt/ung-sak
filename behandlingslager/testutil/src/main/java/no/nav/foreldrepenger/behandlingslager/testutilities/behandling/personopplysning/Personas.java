package no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.PersonInformasjon.Builder;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.k9.kodeverk.geografisk.AdresseType;
import no.nav.k9.kodeverk.geografisk.Landkoder;
import no.nav.k9.kodeverk.geografisk.Region;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.kodeverk.person.SivilstandType;
import no.nav.vedtak.konfig.Tid;

public class Personas {
    private Builder builder;
    private AktørId aktørId;
    private no.nav.foreldrepenger.behandlingslager.testutilities.behandling.personopplysning.Personopplysning.Builder persInfoBuilder;
    private LocalDate fødselsdato;
    private LocalDate dødsdato;

    public Personas(Builder builder) {
        this.builder = builder;
        this.persInfoBuilder = Personopplysning.builder();
    }

    private Personas voksenPerson(AktørId aktørId, SivilstandType st, NavBrukerKjønn kjønn, Region region) {
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
            .fødselsdato(fødselsdato)
            .sivilstand(st)
            .region(region));
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
            .brukerKjønn(NavBrukerKjønn.MANN)
            .sivilstand(SivilstandType.UOPPGITT)
            .region(Region.NORDEN));
        return this;
    }

    public Personas dødsdato(LocalDate dødsdato) {
        this.persInfoBuilder.dødsdato(dødsdato);
        return this;
    }

    public Personas statsborgerskap(Landkoder landkode) {
        // NB: logik her tilsvarer at dersom dødsdato skal settes bør det settes før.
        return statsborgerskap(landkode, fødselsdato, dødsdato);
    }

    public Personas statsborgerskap(Landkoder landkode, LocalDate fom, LocalDate tom) {
        builder.leggTilStatsborgerskap(
            Statsborgerskap.builder().aktørId(aktørId).statsborgerskap(landkode).periode(fom, tom == null ? Tid.TIDENES_ENDE : tom));
        return this;
    }

    public Personas personstatus(PersonstatusType personstatus) {
        return personstatus(personstatus, fødselsdato, dødsdato);
    }

    public Personas personstatus(PersonstatusType personstatus, LocalDate fom, LocalDate tom) {
        builder.leggTilPersonstatus(Personstatus.builder().aktørId(aktørId).personstatus(personstatus).periode(fom, tom == null ? Tid.TIDENES_ENDE : tom));
        return this;
    }

    public Personas kvinne(AktørId aktørId, SivilstandType st) {
        voksenPerson(aktørId, st, NavBrukerKjønn.KVINNE, Region.NORDEN);
        return this;
    }

    public Personas kvinne(AktørId aktørId, SivilstandType st, Region region) {
        voksenPerson(aktørId, st, NavBrukerKjønn.KVINNE, region);
        return this;
    }

    public Personas mann(AktørId aktørId, SivilstandType st) {
        voksenPerson(aktørId, st, NavBrukerKjønn.MANN, Region.NORDEN);
        return this;
    }

    public Personas adresse(AdresseType adresseType, PersonAdresse.Builder adresseBuilder) {
        adresseBuilder.aktørId(aktørId);

        if (adresseBuilder.getPeriode() == null) {
            // for test formål
            adresseBuilder.periode(LocalDate.of(2000, 1, 1), Tid.TIDENES_ENDE);
        }
        adresseBuilder.adresseType(adresseType);
        builder.leggTilAdresser(adresseBuilder);
        return this;
    }

    public Personas mann(AktørId aktørId, SivilstandType st, Region region) {
        voksenPerson(aktørId, st, NavBrukerKjønn.MANN, region);
        return this;
    }

    public PersonInformasjon build() {
        return builder.build();
    }

    public Personas relasjonTil(AktørId tilAktørId, RelasjonsRolleType rolle) {
        Boolean sammeBosted = true;
        return relasjonTil(tilAktørId, rolle, sammeBosted);
    }

    public Personas relasjonTil(AktørId tilAktørId, RelasjonsRolleType rolle, Boolean sammeBosted) {
        builder.leggTilRelasjon(PersonRelasjon.builder().fraAktørId(aktørId).tilAktørId(tilAktørId).relasjonsrolle(rolle).harSammeBosted(sammeBosted));
        return this;
    }

}
