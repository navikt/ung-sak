package no.nav.k9.sak.test.util.fagsak;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;
import no.nav.k9.sak.typer.Saksnummer;

/**
 * Builder for å bygge enkle fagsaker. Primært for test.
 */
public class FagsakBuilder {

    private Saksnummer saksnummer;

    private Fagsak fagsak;

    private FagsakYtelseType fagsakYtelseType;
    private AktørId aktørId = AktørId.dummy();

    private FagsakBuilder(FagsakYtelseType fagsakYtelseType) {
        this.fagsakYtelseType = fagsakYtelseType;
    }

    private FagsakBuilder(Fagsak fagsak) {
        this.fagsak = fagsak;
    }

    public static FagsakBuilder enkel(Fagsak fagsak) {
        return new FagsakBuilder(fagsak);
    }

    public static FagsakBuilder nyFagsak(FagsakYtelseType fagsakYtelseType) {
        return new FagsakBuilder(fagsakYtelseType);
    }

    public static FagsakBuilder nyEngangstønad() {
        return new FagsakBuilder(FagsakYtelseType.ENGANGSTØNAD);
    }

    public static FagsakBuilder nyForeldrepengesak() {
        return new FagsakBuilder(FagsakYtelseType.FORELDREPENGER);
    }

    public AktørId getAktørId() {
        return aktørId;
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

    public FagsakBuilder medBruker(AktørId aktørId) {
        validerFagsakIkkeSatt();
        this.aktørId = aktørId;
        return this;
    }

    public Fagsak build() {

        if (fagsak != null) {
            return fagsak;
        } else {
            fagsak = Fagsak.opprettNy(fagsakYtelseType, aktørId, saksnummer);
            return fagsak;
        }

    }
}
