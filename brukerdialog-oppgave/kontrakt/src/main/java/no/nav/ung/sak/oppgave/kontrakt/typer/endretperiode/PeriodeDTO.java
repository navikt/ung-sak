package no.nav.ung.sak.oppgave.kontrakt.typer.endretperiode;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.Objects;

public class PeriodeDTO {

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("fomDato")
    private LocalDate fomDato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("tomDato")
    private LocalDate tomDato;

    public PeriodeDTO() {
    }

    public PeriodeDTO(LocalDate fomDato, LocalDate tomDato) {
        this.fomDato = fomDato;
        this.tomDato = tomDato;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public void setFomDato(LocalDate fomDato) {
        this.fomDato = fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public void setTomDato(LocalDate tomDato) {
        this.tomDato = tomDato;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeriodeDTO that = (PeriodeDTO) o;
        return Objects.equals(fomDato, that.fomDato) &&
               Objects.equals(tomDato, that.tomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fomDato, tomDato);
    }
}

