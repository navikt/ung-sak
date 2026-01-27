package no.nav.ung.sak.oppgave.typer.kontrollerregisterinntekt;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.kodeverk.arbeidsforhold.OverordnetInntektYtelseType;

import java.util.Objects;

public class YtelseRegisterInntektData {

    @JsonProperty("inntekt")
    private int inntekt;

    @JsonProperty("ytelsetype")
    private OverordnetInntektYtelseType ytelsetype;

    public YtelseRegisterInntektData() {
    }

    public YtelseRegisterInntektData(int inntekt, OverordnetInntektYtelseType ytelsetype) {
        this.inntekt = inntekt;
        this.ytelsetype = ytelsetype;
    }

    public int getInntekt() {
        return inntekt;
    }

    public void setInntekt(int inntekt) {
        this.inntekt = inntekt;
    }

    public OverordnetInntektYtelseType getYtelsetype() {
        return ytelsetype;
    }

    public void setYtelsetype(OverordnetInntektYtelseType ytelsetype) {
        this.ytelsetype = ytelsetype;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YtelseRegisterInntektData that = (YtelseRegisterInntektData) o;
        return inntekt == that.inntekt &&
               ytelsetype == that.ytelsetype;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inntekt, ytelsetype);
    }
}

