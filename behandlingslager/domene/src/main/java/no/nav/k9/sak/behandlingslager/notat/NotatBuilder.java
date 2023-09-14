package no.nav.k9.sak.behandlingslager.notat;

import java.util.Objects;
import java.util.UUID;

import no.nav.k9.kodeverk.behandling.FagsakYtelseType;
import no.nav.k9.sak.behandlingslager.fagsak.Fagsak;
import no.nav.k9.sak.typer.AktørId;

public class NotatBuilder {
    private UUID uuid;
    private String notatTekst;
    private AktørId gjelder;
    private Long fagsakId;
    private boolean skjult = false;
    private FagsakYtelseType ytelseType;

    private NotatBuilder() {}

    public static NotatBuilder of(Fagsak fagsak, boolean gjelderPleietrengende) {
        var builder = new NotatBuilder();
        if (gjelderPleietrengende) {
            builder.gjelder = fagsak.getPleietrengendeAktørId();
            builder.ytelseType = fagsak.getYtelseType();
        } else {
            builder.fagsakId = fagsak.getId();
        }

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
        if (gjelder != null) {
            if (fagsakId != null) throw new IllegalArgumentException("Kan ikke sette både fagsak og aktør");
            return new NotatAktørEntitet(gjelder, ytelseType, notatTekst, skjult);
        }
        if (fagsakId != null) {
            return new NotatSakEntitet(fagsakId, notatTekst, skjult);
        }

        throw new IllegalArgumentException("Verken gjelder eller fagsak er satt");
    }
}
