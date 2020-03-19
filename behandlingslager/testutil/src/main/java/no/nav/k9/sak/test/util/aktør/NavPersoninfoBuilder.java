package no.nav.k9.sak.test.util.aktør;

import static java.time.Month.OCTOBER;
import static no.nav.k9.kodeverk.person.NavBrukerKjønn.KVINNE;

import java.time.LocalDate;

import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.PersonstatusType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;

public class NavPersoninfoBuilder {

    private static final String DEFAULT_NAVN = "Anne-Berit Hjartdal";
    private static final AktørId DEFAULT_AKTØR_ID = AktørId.dummy();
    private static final PersonIdent DEFAULT_FNR = PersonIdent.fra("13107221234");
    private static final LocalDate DEFAULT_FØDSELDATO = LocalDate.of(1972, OCTOBER, 13);
    private static final Språkkode DEFAULT_FORETRUKKET_SPRÅK = Språkkode.nb;
    private static final String DEFAULT_DISKRESJONSKODE = "6";
    private static final PersonstatusType DEFAULT_PERSONSTATUSTYPE = PersonstatusType.BOSA;
    private NavBrukerKjønn kjønn = KVINNE;

    private AktørId aktørId;
    private PersonIdent personIdent;
    private String navn;
    private LocalDate fødselsdato;
    private Språkkode foretrukketSpråk;

    private String diskresjonskode;
    private PersonstatusType personstatusType;
    public NavPersoninfoBuilder() {
    }


    public NavPersoninfoBuilder medAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
        return this;
    }
    
    public NavPersoninfoBuilder medPersonIdent(PersonIdent personIdent) {
        this.personIdent = personIdent;
        return this;
    }

    public NavPersoninfoBuilder medNavn(String navn) {
        this.navn = navn;
        return this;
    }

    public NavPersoninfoBuilder medFødselsdato(LocalDate fødselsdato) {
        this.fødselsdato = fødselsdato;
        return this;
    }

    public NavPersoninfoBuilder medForetrukketSpråk(Språkkode foretrukketSpråk) {
        this.foretrukketSpråk = foretrukketSpråk;
        return this;
    }

    public NavPersoninfoBuilder medDiskresjonskode(String diskresjonskode) {
        this.diskresjonskode = diskresjonskode;
        return this;
    }
    public NavPersoninfoBuilder medPersonstatusType(PersonstatusType personstatusType) {
        this.personstatusType = personstatusType;
        return this;
    }

    public Personinfo build() {
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
        if (personstatusType == null) {
            personstatusType = DEFAULT_PERSONSTATUSTYPE;
        }
        return new Personinfo.Builder()
            .medAktørId(aktørId)
            .medPersonIdent(personIdent)
            .medNavn(navn)
            .medFødselsdato(fødselsdato)
            .medKjønn(kjønn)
            .medDiskresjonsKode(diskresjonskode)
            .medPersonstatusType(personstatusType)
            .medForetrukketSpråk(foretrukketSpråk)
            .build();
    }
}
