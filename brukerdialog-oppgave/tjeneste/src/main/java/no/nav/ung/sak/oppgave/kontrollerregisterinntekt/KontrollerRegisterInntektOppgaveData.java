package no.nav.ung.sak.oppgave.kontrollerregisterinntekt;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.oppgave.OppgaveData;

import java.time.LocalDate;
import java.util.Objects;

public class KontrollerRegisterInntektOppgaveData extends OppgaveData {

    @JsonProperty("registerinntekt")
    private RegisterinntektData registerinntekt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("fomDato")
    private LocalDate fomDato;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("tomDato")
    private LocalDate tomDato;

    @JsonProperty("gjelderDelerAvMåned")
    private boolean gjelderDelerAvMåned;

    public KontrollerRegisterInntektOppgaveData() {
    }

    public KontrollerRegisterInntektOppgaveData(RegisterinntektData registerinntekt, LocalDate fomDato, LocalDate tomDato, boolean gjelderDelerAvMåned) {
        this.registerinntekt = registerinntekt;
        this.fomDato = fomDato;
        this.tomDato = tomDato;
        this.gjelderDelerAvMåned = gjelderDelerAvMåned;
    }

    public RegisterinntektData getRegisterinntekt() {
        return registerinntekt;
    }

    public void setRegisterinntekt(RegisterinntektData registerinntekt) {
        this.registerinntekt = registerinntekt;
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

    public boolean isGjelderDelerAvMåned() {
        return gjelderDelerAvMåned;
    }

    public void setGjelderDelerAvMåned(boolean gjelderDelerAvMåned) {
        this.gjelderDelerAvMåned = gjelderDelerAvMåned;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KontrollerRegisterInntektOppgaveData that = (KontrollerRegisterInntektOppgaveData) o;
        return gjelderDelerAvMåned == that.gjelderDelerAvMåned &&
               Objects.equals(registerinntekt, that.registerinntekt) &&
               Objects.equals(fomDato, that.fomDato) &&
               Objects.equals(tomDato, that.tomDato);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registerinntekt, fomDato, tomDato, gjelderDelerAvMåned);
    }
}

