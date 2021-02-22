package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.uttak;

import java.util.Objects;
import java.util.Set;

import no.nav.k9.sak.typer.JournalpostId;

public class UttakPerioderDokumenter {

    private JournalpostId journalpostId;
    private Set<ArbeidPeriode> søknadsperioder;

    public UttakPerioderDokumenter(JournalpostId journalpostId, Set<ArbeidPeriode> søknadsperioder) {
        this.journalpostId = journalpostId;
        this.søknadsperioder = søknadsperioder;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Set<ArbeidPeriode> getSøknadsperioder() {
        return søknadsperioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UttakPerioderDokumenter that = (UttakPerioderDokumenter) o;
        return journalpostId.equals(that.journalpostId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(journalpostId);
    }

    @Override
    public String toString() {
        return "SøknadsPeriodeDokumenter{" +
            "journalpostId=" + journalpostId +
            ", søknadsperioder=" + søknadsperioder +
            '}';
    }
}
