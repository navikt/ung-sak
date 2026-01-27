package no.nav.ung.sak.oppgave.oppgavedata;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.oppgave.OppgaveData;

import java.time.LocalDate;
import java.util.Objects;

public class EndretSluttdatoOppgaveData extends OppgaveData {

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("nySluttdato")
    private LocalDate nySluttdato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("forrigeSluttdato")
    private LocalDate forrigeSluttdato;

    public EndretSluttdatoOppgaveData() {
    }

    public EndretSluttdatoOppgaveData(LocalDate nySluttdato, LocalDate forrigeSluttdato) {
        this.nySluttdato = nySluttdato;
        this.forrigeSluttdato = forrigeSluttdato;
    }

    public LocalDate getNySluttdato() {
        return nySluttdato;
    }

    public void setNySluttdato(LocalDate nySluttdato) {
        this.nySluttdato = nySluttdato;
    }

    public LocalDate getForrigeSluttdato() {
        return forrigeSluttdato;
    }

    public void setForrigeSluttdato(LocalDate forrigeSluttdato) {
        this.forrigeSluttdato = forrigeSluttdato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndretSluttdatoOppgaveData that = (EndretSluttdatoOppgaveData) o;
        return Objects.equals(nySluttdato, that.nySluttdato) &&
               Objects.equals(forrigeSluttdato, that.forrigeSluttdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nySluttdato, forrigeSluttdato);
    }
}

