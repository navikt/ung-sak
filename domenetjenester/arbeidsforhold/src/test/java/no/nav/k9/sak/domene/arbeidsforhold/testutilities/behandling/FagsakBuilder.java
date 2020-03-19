package no.nav.k9.sak.domene.arbeidsforhold.testutilities.behandling;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.aktør.NavBruker;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * Builder for å bygge enkle fagsaker. Primært for test.
 */
class FagsakBuilder {

    private Saksnummer saksnummer;

    private NavBrukerBuilder brukerBuilder = new NavBrukerBuilder();

    private Fagsak fagsak;

    private FagsakYtelseType fagsakYtelseType;

    private FagsakBuilder(FagsakYtelseType fagsakYtelseType) {
        this.fagsakYtelseType = fagsakYtelseType;
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
        brukerBuilder.medAktørId(aktørId);
        return this;
    }

    NavBrukerBuilder getBrukerBuilder() {
        return brukerBuilder;
    }

    FagsakBuilder medBruker(NavBruker bruker) {
        validerFagsakIkkeSatt();
        brukerBuilder.medBruker(bruker);
        return this;
    }

    static FagsakBuilder nyFagsak(FagsakYtelseType fagsakYtelseType) {
        return new FagsakBuilder(fagsakYtelseType);
    }

    Fagsak build() {
        if (fagsak != null) {
            return fagsak;
        } else {
            fagsak = Fagsak.opprettNy(fagsakYtelseType, brukerBuilder.build(), saksnummer);
            return fagsak;
        }
    }
}
