package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Saksnummer;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class TilgrensendeYtelserDto implements Comparable<TilgrensendeYtelserDto> {

    @JsonProperty(value = "periodeFraDato")
    @NotNull
    private LocalDate periodeFraDato;

    @JsonProperty(value = "periodeTilDato")
    @NotNull
    private LocalDate periodeTilDato;

    @JsonProperty(value = "relatertYtelseType")
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String relatertYtelseType;

    @JsonProperty(value = "saksNummer")
    @JsonAlias(value = "saksnummer")
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}\\-_/:\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String saksNummer;

    @JsonProperty(value = "status")
    @NotNull
    @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{Sc}\\p{L}\\p{N}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'")
    private String status;

    public TilgrensendeYtelserDto() {
        //
    }

    @Override
    public int compareTo(TilgrensendeYtelserDto other) {
        return Comparator.nullsLast(LocalDate::compareTo).compare(other.getPeriodeFraDato(), this.getPeriodeFraDato());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TilgrensendeYtelserDto)) {
            return false;
        }
        TilgrensendeYtelserDto that = (TilgrensendeYtelserDto) o;
        return Objects.equals(relatertYtelseType, that.relatertYtelseType) &&
            Objects.equals(periodeFraDato, that.periodeFraDato) &&
            Objects.equals(periodeTilDato, that.periodeTilDato) &&
            Objects.equals(status, that.status) &&
            Objects.equals(saksNummer, that.saksNummer);
    }

    public LocalDate getPeriodeFraDato() {
        return periodeFraDato;
    }

    public LocalDate getPeriodeTilDato() {
        return periodeTilDato;
    }

    public String getRelatertYtelseType() {
        return relatertYtelseType;
    }

    public String getSaksNummer() {
        return saksNummer;
    }

    public String getStatus() {
        return status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(relatertYtelseType, periodeFraDato, periodeTilDato, status, saksNummer);
    }

    public void setPeriodeFraDato(LocalDate periodeFraDato) {
        this.periodeFraDato = periodeFraDato;
    }

    public void setPeriodeTilDato(LocalDate periodeTilDato) {
        this.periodeTilDato = periodeTilDato;
    }

    public void setRelatertYtelseType(String relatertYtelseType) {
        this.relatertYtelseType = relatertYtelseType;
    }

    public void setSaksNummer(Saksnummer saksNummer) {
        if (saksNummer != null) {
            this.saksNummer = saksNummer.getVerdi();
        }
    }

    public void setSaksNummer(String saksNummer) {
        this.saksNummer = saksNummer;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
