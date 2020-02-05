package no.nav.foreldrepenger.mottak.vurderfagsystem.impl;

import java.time.LocalDate;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.aktør.Personinfo;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.PersonIdent;
import no.nav.k9.sak.typer.Saksnummer;

public class BehandlingslagerTestUtil {


    private BehandlingslagerTestUtil() {
    }

    public static final Fagsak buildFagsak(final Long fagsakid, final boolean erAvsluttet, FagsakYtelseType ytelseType) {
        NavBruker bruker = lagNavBruker();
        Fagsak fagsak = Fagsak.opprettNy(ytelseType, bruker, new Saksnummer(fagsakid * 2 + ""));
        fagsak.setId(fagsakid);
        if (erAvsluttet) {
            fagsak.setAvsluttet();
        }
        return fagsak;
    }

    public static final NavBruker lagNavBruker() {
        Personinfo.Builder personinfoBuilder = new Personinfo.Builder();
        personinfoBuilder.medAktørId(AktørId.dummy());
        personinfoBuilder.medPersonIdent(new PersonIdent("01017012345"));
        personinfoBuilder.medNavn("Tjoms");
        personinfoBuilder.medFødselsdato(LocalDate.now());
        personinfoBuilder.medKjønn(NavBrukerKjønn.KVINNE);
        Personinfo personinfo = personinfoBuilder.build();

        NavBruker navBruker = NavBruker.opprettNy(personinfo);
        return navBruker;
    }

}
