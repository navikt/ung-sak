package no.nav.ung.sak.oppgave.varsel.oppgavedata;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.oppgave.OppgaveData;
import no.nav.ung.sak.oppgave.varsel.PeriodeDTO;
import no.nav.ung.sak.oppgave.varsel.PeriodeEndringType;

import java.util.Objects;
import java.util.Set;

public class EndretPeriodeOppgaveData extends OppgaveData {

    @JsonProperty("nyPeriode")
    private PeriodeDTO nyPeriode;

    @JsonProperty("forrigePeriode")
    private PeriodeDTO forrigePeriode;

    @JsonProperty("endringer")
    private Set<PeriodeEndringType> endringer;

    public EndretPeriodeOppgaveData() {
    }

    public EndretPeriodeOppgaveData(PeriodeDTO nyPeriode, PeriodeDTO forrigePeriode, Set<PeriodeEndringType> endringer) {
        this.nyPeriode = nyPeriode;
        this.forrigePeriode = forrigePeriode;
        this.endringer = endringer;
    }

    public PeriodeDTO getNyPeriode() {
        return nyPeriode;
    }

    public void setNyPeriode(PeriodeDTO nyPeriode) {
        this.nyPeriode = nyPeriode;
    }

    public PeriodeDTO getForrigePeriode() {
        return forrigePeriode;
    }

    public void setForrigePeriode(PeriodeDTO forrigePeriode) {
        this.forrigePeriode = forrigePeriode;
    }

    public Set<PeriodeEndringType> getEndringer() {
        return endringer;
    }

    public void setEndringer(Set<PeriodeEndringType> endringer) {
        this.endringer = endringer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndretPeriodeOppgaveData that = (EndretPeriodeOppgaveData) o;
        return Objects.equals(nyPeriode, that.nyPeriode) &&
               Objects.equals(forrigePeriode, that.forrigePeriode) &&
               Objects.equals(endringer, that.endringer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nyPeriode, forrigePeriode, endringer);
    }
}

