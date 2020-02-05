package no.nav.folketrygdloven.beregningsgrunnlag.testutilities.behandling;

import no.nav.folketrygdloven.beregningsgrunnlag.testutilities.aktør.NavBrukerBuilder;
import no.nav.foreldrepenger.behandlingslager.aktør.NavBruker;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.kodeverk.person.NavBrukerKjønn;
import no.nav.k9.kodeverk.person.RelasjonsRolleType;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * Builder for å bygge enkle fagsaker. Primært for test.
 */
public class FagsakBuilder {

    private Saksnummer saksnummer;

    private NavBrukerBuilder brukerBuilder = new NavBrukerBuilder();

    private RelasjonsRolleType rolle;

    private Fagsak fagsak;

    private FagsakYtelseType fagsakYtelseType;

    private FagsakBuilder(RelasjonsRolleType rolle, FagsakYtelseType fagsakYtelseType) {
        this.rolle = rolle;
        this.fagsakYtelseType = fagsakYtelseType;
    }

    private FagsakBuilder(Fagsak fagsak) {
        this.fagsak = fagsak;
    }

    public FagsakBuilder medSaksnummer(Saksnummer saksnummer) {
        validerFagsakIkkeSatt();
        this.saksnummer = saksnummer;
        return this;
    }

    private void validerFagsakIkkeSatt() {
        if (fagsak != null) {
            throw new IllegalStateException("Fagsak er allerede konfigurert, kan ikke overstyre her");
        }
    }

    public FagsakBuilder medBrukerAktørId(AktørId aktørId) {
        validerFagsakIkkeSatt();
        brukerBuilder.medAktørId(aktørId);
        return this;
    }

    public FagsakBuilder medBrukerKjønn(NavBrukerKjønn kjønn) {
        validerFagsakIkkeSatt();
        brukerBuilder.medKjønn(kjønn);
        return this;
    }

    public NavBrukerBuilder getBrukerBuilder() {
        return brukerBuilder;
    }

    public RelasjonsRolleType getRolle() {
        return rolle;
    }

    public FagsakBuilder medBruker(NavBruker bruker) {
        validerFagsakIkkeSatt();
        brukerBuilder.medBruker(bruker);
        return this;
    }

    public static FagsakBuilder nyFagsak(FagsakYtelseType fagsakYtelseType, RelasjonsRolleType rolle) {
        return new FagsakBuilder(rolle, fagsakYtelseType);
    }

    public Fagsak build() {

        if (fagsak != null) {
            return fagsak;
        } else {
            fagsak = Fagsak.opprettNy(fagsakYtelseType, brukerBuilder.build(), saksnummer);
            return fagsak;
        }

    }
}
