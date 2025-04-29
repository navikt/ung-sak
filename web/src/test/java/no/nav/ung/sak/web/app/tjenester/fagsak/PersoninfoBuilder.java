package no.nav.ung.sak.web.app.tjenester.fagsak;

import static java.time.Month.OCTOBER;

import java.time.LocalDate;

import no.nav.ung.kodeverk.geografisk.Språkkode;
import no.nav.ung.sak.behandlingslager.aktør.Personinfo;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.PersonIdent;

class PersoninfoBuilder {

    private static final String DEFAULT_NAVN = "Anne-Berit Hjartdal";
    private static final AktørId DEFAULT_AKTØR_ID = AktørId.dummy();
    private static final PersonIdent DEFAULT_FNR = PersonIdent.fra("13107221234");
    private static final LocalDate DEFAULT_FØDSELDATO = LocalDate.of(1972, OCTOBER, 13);
    private static final Språkkode DEFAULT_FORETRUKKET_SPRÅK = Språkkode.nb;
    private static final String DEFAULT_DISKRESJONSKODE = "6";

    private AktørId aktørId;
    private PersonIdent personIdent;
    private String navn;
    private LocalDate fødselsdato;
    private Språkkode foretrukketSpråk;

    private String diskresjonskode;

    PersoninfoBuilder() {
    }


    PersoninfoBuilder medAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
        return this;
    }

    PersoninfoBuilder medPersonIdent(PersonIdent personIdent) {
        this.personIdent = personIdent;
        return this;
    }

    PersoninfoBuilder medNavn(String navn) {
        this.navn = navn;
        return this;
    }

    PersoninfoBuilder medFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
        return this;
    }

    PersoninfoBuilder medForetrukketSpråk(Språkkode foretrukketSpråk) {
        this.foretrukketSpråk = foretrukketSpråk;
        return this;
    }

    PersoninfoBuilder medDiskresjonskode(String diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
        return this;
    }

    Personinfo build() {
        if (aktørId == null) {
            aktørId = DEFAULT_AKTØR_ID;
        }
        if (personIdent == null) {
            personIdent = DEFAULT_FNR;
        }
        if (navn == null) {
            navn = DEFAULT_NAVN;
        }
        if (fødselsdato == null) {
            fødselsdato = DEFAULT_FØDSELDATO;
        }
        if (foretrukketSpråk == null) {
            foretrukketSpråk = DEFAULT_FORETRUKKET_SPRÅK;
        }
        if (diskresjonskode == null) {
            diskresjonskode = DEFAULT_DISKRESJONSKODE;
        }

        return new Personinfo.Builder()
            .medAktørId(aktørId)
            .medPersonIdent(personIdent)
            .medNavn(navn)
            .medFødselsdato(fødselsdato)
            .medDiskresjonsKode(diskresjonskode)
            .medForetrukketSpråk(foretrukketSpråk)
            .build();
    }
}
