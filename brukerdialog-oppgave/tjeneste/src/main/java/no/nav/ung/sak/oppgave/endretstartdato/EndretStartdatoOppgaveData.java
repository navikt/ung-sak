package no.nav.ung.sak.oppgave.endretstartdato;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.oppgave.OppgaveData;

import java.time.LocalDate;
import java.util.Objects;

public class EndretStartdatoOppgaveData extends OppgaveData {

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("nyStartdato")
    private LocalDate nyStartdato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("forrigeStartdato")
    private LocalDate forrigeStartdato;

    public EndretStartdatoOppgaveData() {
    }

    public EndretStartdatoOppgaveData(LocalDate nyStartdato, LocalDate forrigeStartdato) {
        this.nyStartdato = nyStartdato;
        this.forrigeStartdato = forrigeStartdato;
    }

    public LocalDate getNyStartdato() {
        return nyStartdato;
    }

    public void setNyStartdato(LocalDate nyStartdato) {
        this.nyStartdato = nyStartdato;
    }

    public LocalDate getForrigeStartdato() {
        return forrigeStartdato;
    }

    public void setForrigeStartdato(LocalDate forrigeStartdato) {
        this.forrigeStartdato = forrigeStartdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndretStartdatoOppgaveData that = (EndretStartdatoOppgaveData) o;
        return Objects.equals(nyStartdato, that.nyStartdato) &&
               Objects.equals(forrigeStartdato, that.forrigeStartdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nyStartdato, forrigeStartdato);
    }
}

