package no.nav.ung.sak.domene.arbeidsforhold.testutilities.behandling;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

/**
 * Builder for å bygge enkle fagsaker. Primært for test.
 */
class FagsakBuilder {

    private Saksnummer saksnummer;

    private AktørId bruker = AktørId.dummy();

    private Fagsak fagsak;

    private final FagsakYtelseType fagsakYtelseType;

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
