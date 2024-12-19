package no.nav.ung.sak.behandlingslager.notat;

import java.util.Objects;
import java.util.UUID;

import no.nav.ung.kodeverk.behandling.FagsakYtelseType;
import no.nav.ung.sak.behandlingslager.fagsak.Fagsak;
import no.nav.ung.sak.typer.AktørId;

public class NotatBuilder {
    private UUID uuid;
    private String notatTekst;
    private Long fagsakId;
    private boolean skjult = false;
    private FagsakYtelseType ytelseType;

    private NotatBuilder() {
    }

    public static NotatBuilder of(Fagsak fagsak) {
        var builder = new NotatBuilder();
        builder.fagsakId = fagsak.getId();
        return builder;
    }

    public NotatBuilder notatTekst(String notatTekst) {
        this.notatTekst = notatTekst;
        return this;
    }

    public NotatBuilder skjult(boolean skjult) {
        this.skjult = skjult;
        return this;
    }

    public NotatEntitet build() {
        Objects.requireNonNull(notatTekst, "Notattekst må være satt");
        if (fagsakId != null) {
            return new NotatSakEntitet(fagsakId, notatTekst, skjult);
        }

        throw new IllegalArgumentException("Verken aktør eller fagsak er satt");
    }
}
