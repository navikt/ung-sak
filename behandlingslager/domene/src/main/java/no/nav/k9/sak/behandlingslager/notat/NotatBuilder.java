package no.nav.k9.sak.behandlingslager.notat;

import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;

public class NotatBuilder {
    private Long id;
    private String notatTekst;
    private AktørId gjelder;
    private Long fagsakId;
    private boolean skjult;

    public static NotatBuilder forFagsak(Fagsak fagsak, boolean gjelderPleietrengende) {
        return new NotatBuilder()
            .fagsakId(fagsak.getId())
            .gjelder(gjelderPleietrengende ? fagsak.getPleietrengendeAktørId() : fagsak.getAktørId());
    }

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
