package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.time.LocalDate;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.kodeverk.arbeidsforhold.ArbeidType;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class AndreYtelserDto {

    @JsonAlias("fom")
    @JsonProperty(value = "periodeFom", required = true)
    @NotNull
    private LocalDate periodeFom;

    @JsonProperty(value = "periodeTom", required = true)
    @NotNull
    private LocalDate periodeTom;

    @JsonProperty(value = "ytelseType", required = true)
    @NotNull
    @Valid
    private ArbeidType ytelseType;

    public LocalDate getPeriodeFom() {
        return periodeFom;
    }

    public LocalDate getPeriodeTom() {
        return periodeTom;
    }

    public ArbeidType getYtelseType() {
        return ytelseType;
    }

    public void setPeriodeFom(LocalDate periodeFom) {
        this.periodeFom = periodeFom;
    }

    public void setPeriodeTom(LocalDate periodeTom) {
        this.periodeTom = periodeTom;
    }

    public void setYtelseType(ArbeidType ytelseType) {
        this.ytelseType = ytelseType;
    }
}
