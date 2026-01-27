package no.nav.ung.sak.oppgave.oppgavedata;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class ArbeidOgFrilansRegisterInntektData {

    @JsonProperty("inntekt")
    private int inntekt;

    @JsonProperty("arbeidsgiver")
    private String arbeidsgiver;

    public ArbeidOgFrilansRegisterInntektData() {
    }

    public ArbeidOgFrilansRegisterInntektData(int inntekt, String arbeidsgiver) {
        this.inntekt = inntekt;
        this.arbeidsgiver = arbeidsgiver;
    }

    public int getInntekt() {
        return inntekt;
    }

    public void setInntekt(int inntekt) {
        this.inntekt = inntekt;
    }

    public String getArbeidsgiver() {
        return arbeidsgiver;
    }

    public void setArbeidsgiver(String arbeidsgiver) {
        this.arbeidsgiver = arbeidsgiver;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArbeidOgFrilansRegisterInntektData that = (ArbeidOgFrilansRegisterInntektData) o;
        return inntekt == that.inntekt &&
               Objects.equals(arbeidsgiver, that.arbeidsgiver);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inntekt, arbeidsgiver);
    }
}

