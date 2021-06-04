package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.etablerttilsyn.delt;

import java.time.Duration;
import java.util.Objects;

import no.nav.k9.sak.kontrakt.tilsyn.Kilde;
import no.nav.k9.sak.typer.JournalpostId;

public class UtledetEtablertTilsyn {

    private Duration varighet;
    private Kilde kilde;
    private JournalpostId journalpostId;


    public UtledetEtablertTilsyn(Duration varighet, Kilde kilde, JournalpostId journalpostId) {
        this.varighet = varighet;
        this.kilde = kilde;
        this.journalpostId = journalpostId;
    }

    public Duration getVarighet() {
        return varighet;
    }

    public Kilde getKilde() {
        return kilde;
    }
    
    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UtledetEtablertTilsyn that = (UtledetEtablertTilsyn) o;
        return Objects.equals(varighet, that.varighet) && kilde == that.kilde;
    }

    @Override
    public int hashCode() {
        return Objects.hash(varighet, kilde);
    }
}
