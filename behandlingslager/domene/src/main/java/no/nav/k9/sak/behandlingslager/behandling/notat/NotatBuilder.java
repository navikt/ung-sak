package no.nav.k9.sak.behandlingslager.behandling.notat;

import no.nav.k9.sak.typer.AktørId;

public class NotatBuilder {
    private Long id;
    private String notatTekst;
    private AktørId gjelder;
    private Long fagsakId;
    private boolean skjult;

    public NotatBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public NotatBuilder notatTekst(String notatTekst) {
        this.notatTekst = notatTekst;
        return this;
    }

    public NotatBuilder gjelder(AktørId gjelder) {
        this.gjelder = gjelder;
        return this;
    }

    public NotatBuilder fagsakId(Long fagsakId) {
        this.fagsakId = fagsakId;
        return this;
    }

    public NotatBuilder skjult(boolean skjult) {
        this.skjult = skjult;
        return this;
    }

    public NotatEntitet build() {
        return new NotatEntitet(id, notatTekst, gjelder, fagsakId, skjult);
    }
}
