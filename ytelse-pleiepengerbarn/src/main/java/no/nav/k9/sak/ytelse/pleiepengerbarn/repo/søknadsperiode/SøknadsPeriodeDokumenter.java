package no.nav.k9.sak.ytelse.pleiepengerbarn.repo.søknadsperiode;

import java.util.Objects;
import java.util.Set;

import no.nav.k9.sak.domene.uttak.repo.Søknadsperiode;
import no.nav.k9.sak.typer.JournalpostId;

public class SøknadsPeriodeDokumenter {

    private JournalpostId journalpostId;
    private Set<Søknadsperiode> søknadsperioder;

    public SøknadsPeriodeDokumenter(JournalpostId journalpostId, Set<Søknadsperiode> søknadsperioder) {
        this.journalpostId = journalpostId;
        this.søknadsperioder = søknadsperioder;
    }

    public JournalpostId getJournalpostId() {
        return journalpostId;
    }

    public Set<Søknadsperiode> getSøknadsperioder() {
        return søknadsperioder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SøknadsPeriodeDokumenter that = (SøknadsPeriodeDokumenter) o;
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
