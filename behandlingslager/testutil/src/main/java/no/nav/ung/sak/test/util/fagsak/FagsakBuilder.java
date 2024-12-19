package no.nav.ung.sak.test.util.fagsak;

import java.time.LocalDate;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.typer.AktørId;
import no.nav.ung.sak.typer.Saksnummer;

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
            fagsak = Fagsak.opprettNy(fagsakYtelseType, aktørId, saksnummer, LocalDate.now(), null);
            return fagsak;
        }

    }
}
