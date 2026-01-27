package no.nav.ung.sak.oppgave.søkytelse;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.oppgave.OppgaveData;

import java.time.LocalDate;
import java.util.Objects;

public class SøkYtelseOppgaveData extends OppgaveData {

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("fomDato")
    private LocalDate fomDato;

    public SøkYtelseOppgaveData() {
    }

    public SøkYtelseOppgaveData(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public void setFomDato(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SøkYtelseOppgaveData that = (SøkYtelseOppgaveData) o;
        return Objects.equals(fomDato, that.fomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fomDato);
    }
}

