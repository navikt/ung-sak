package no.nav.k9.sak.test.util.aktør;

import no.nav.k9.kodeverk.geografisk.Språkkode;
import no.nav.k9.sak.behandlingslager.aktør.NavBruker;
import no.nav.k9.sak.behandlingslager.aktør.Personinfo;
import no.nav.k9.sak.typer.AktørId;

public class NavBrukerBuilder {

    private NavBruker bruker;

    private AktørId aktørId = AktørId.dummy();
    private Personinfo personinfo;
    private Språkkode språkkode;

    public NavBrukerBuilder() {
        // default ctor
    }

    public NavBrukerBuilder medAktørId(AktørId aktørId) {
        this.aktørId = aktørId;
        return this;
    }

    public NavBrukerBuilder medBruker(NavBruker bruker) {
        this.bruker = bruker;
        return this;
    }

    public NavBrukerBuilder medPersonInfo(Personinfo personinfo) {
        this.personinfo = personinfo;
        return this;
    }

    public NavBrukerBuilder medForetrukketSpråk(Språkkode språkkode) {
        this.språkkode = språkkode;
        return this;
    }

    public Språkkode getSpråkkode() {
        return språkkode;
    }

    public AktørId getAktørId() {
        if(bruker != null) {
            return bruker.getAktørId();
        }
        return aktørId;
    }

    public NavBruker build() {
        if (bruker != null) {
            return bruker;
        }
        if (personinfo == null) {

            personinfo = new NavPersoninfoBuilder()
                .medAktørId(aktørId)
                .medForetrukketSpråk(språkkode)
                .build();
        }
        return NavBruker.opprettNy(personinfo);
    }
}
