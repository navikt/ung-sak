package no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * Builder for å bygge enkle fagsaker. Primært for test.
 */
class FagsakBuilder {

    private Saksnummer saksnummer;

    private AktørId bruker = AktørId.dummy();

    private Fagsak fagsak;

    private FagsakYtelseType fagsakYtelseType;

    private FagsakBuilder(FagsakYtelseType fagsakYtelseType) {
        this.fagsakYtelseType = fagsakYtelseType;
    }

    static FagsakBuilder nyFagsak(FagsakYtelseType fagsakYtelseType) {
        return new FagsakBuilder(fagsakYtelseType);
    }

    FagsakBuilder medSaksnummer(Saksnummer saksnummer) {
        validerFagsakIkkeSatt();
        this.saksnummer = saksnummer;
        return this;
    }

    private void validerFagsakIkkeSatt() {
        if (fagsak != null) {
            throw new IllegalStateException("Fagsak er allerede konfigurert, kan ikke overstyre her");
        }
    }

    FagsakBuilder medBrukerAktørId(AktørId aktørId) {
        validerFagsakIkkeSatt();
        this.bruker = aktørId;
        return this;
    }

    public AktørId getBruker() {
        return bruker;
    }

    Fagsak build() {
        if (fagsak != null) {
            return fagsak;
        } else {
            fagsak = Fagsak.opprettNy(fagsakYtelseType, bruker, saksnummer);
            return fagsak;
        }
    }
}
