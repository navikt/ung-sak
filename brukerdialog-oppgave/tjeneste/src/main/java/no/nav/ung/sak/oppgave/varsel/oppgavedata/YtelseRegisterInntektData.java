package no.nav.ung.sak.oppgave.varsel.oppgavedata;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.ung.sak.oppgave.varsel.YtelseType;

import java.util.Objects;

public class YtelseRegisterInntektData {

    @JsonProperty("inntekt")
    private int inntekt;

    @JsonProperty("ytelsetype")
    private YtelseType ytelsetype;

    public YtelseRegisterInntektData() {
    }

    public YtelseRegisterInntektData(int inntekt, YtelseType ytelsetype) {
        this.inntekt = inntekt;
        this.ytelsetype = ytelsetype;
    }

    public int getInntekt() {
        return inntekt;
    }

    public void setInntekt(int inntekt) {
        this.inntekt = inntekt;
    }

    public YtelseType getYtelsetype() {
        return ytelsetype;
    }

    public void setYtelsetype(YtelseType ytelsetype) {
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

