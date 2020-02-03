package no.nav.foreldrepenger.domene.arbeidsforhold.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.domene.typer.Saksnummer;

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
