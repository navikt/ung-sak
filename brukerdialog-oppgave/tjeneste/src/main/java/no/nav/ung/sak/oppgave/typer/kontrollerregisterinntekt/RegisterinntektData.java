package no.nav.ung.sak.oppgave.typer.kontrollerregisterinntekt;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class RegisterinntektData {

    @JsonProperty("arbeidOgFrilansInntekter")
    private List<ArbeidOgFrilansRegisterInntektData> arbeidOgFrilansInntekter;

    @JsonProperty("ytelseInntekter")
    private List<YtelseRegisterInntektData> ytelseInntekter;

    public RegisterinntektData() {
    }

    public RegisterinntektData(List<ArbeidOgFrilansRegisterInntektData> arbeidOgFrilansInntekter, List<YtelseRegisterInntektData> ytelseInntekter) {
        this.arbeidOgFrilansInntekter = arbeidOgFrilansInntekter;
        this.ytelseInntekter = ytelseInntekter;
    }

    public List<ArbeidOgFrilansRegisterInntektData> getArbeidOgFrilansInntekter() {
        return arbeidOgFrilansInntekter;
    }

    public void setArbeidOgFrilansInntekter(List<ArbeidOgFrilansRegisterInntektData> arbeidOgFrilansInntekter) {
        this.arbeidOgFrilansInntekter = arbeidOgFrilansInntekter;
    }

    public List<YtelseRegisterInntektData> getYtelseInntekter() {
        return ytelseInntekter;
    }

    public void setYtelseInntekter(List<YtelseRegisterInntektData> ytelseInntekter) {
        this.ytelseInntekter = ytelseInntekter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RegisterinntektData that = (RegisterinntektData) o;
        return Objects.equals(arbeidOgFrilansInntekter, that.arbeidOgFrilansInntekter) &&
               Objects.equals(ytelseInntekter, that.ytelseInntekter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(arbeidOgFrilansInntekter, ytelseInntekter);
    }
}

