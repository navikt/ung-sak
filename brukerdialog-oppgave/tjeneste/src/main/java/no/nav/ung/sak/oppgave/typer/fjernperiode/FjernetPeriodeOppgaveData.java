package no.nav.ung.sak.oppgave.typer.fjernperiode;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.oppgave.OppgaveData;

import java.time.LocalDate;
import java.util.Objects;

public class FjernetPeriodeOppgaveData extends OppgaveData {

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("forrigeStartdato")
    private LocalDate forrigeStartdato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("forrigeSluttdato")
    private LocalDate forrigeSluttdato;

    public FjernetPeriodeOppgaveData() {
    }

    public FjernetPeriodeOppgaveData(LocalDate forrigeStartdato, LocalDate forrigeSluttdato) {
        this.forrigeStartdato = forrigeStartdato;
        this.forrigeSluttdato = forrigeSluttdato;
    }

    public LocalDate getForrigeStartdato() {
        return forrigeStartdato;
    }

    public void setForrigeStartdato(LocalDate forrigeStartdato) {
        this.forrigeStartdato = forrigeStartdato;
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
        FjernetPeriodeOppgaveData that = (FjernetPeriodeOppgaveData) o;
        return Objects.equals(forrigeStartdato, that.forrigeStartdato) &&
               Objects.equals(forrigeSluttdato, that.forrigeSluttdato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(forrigeStartdato, forrigeSluttdato);
    }
}

