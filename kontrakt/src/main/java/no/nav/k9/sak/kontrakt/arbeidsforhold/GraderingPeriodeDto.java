package no.nav.k9.sak.kontrakt.arbeidsforhold;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class GraderingPeriodeDto {

    @JsonProperty(value = "fom", required = true)
    @NotNull
    private LocalDate fom;

    @JsonProperty(value = "tom", required = true)
    @NotNull
    private LocalDate tom;

    @JsonProperty(value = "arbeidsprosent", required = true)
    @NotNull
    @Digits(integer = 3, fraction = 2)
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal arbeidsprosent;

    protected GraderingPeriodeDto() {
        //
    }

    public GraderingPeriodeDto(Periode periode, BigDecimal arbeidstidsprosent) {
        this.fom = periode.getFom();
        this.tom = periode.getTom();
        this.arbeidsprosent = Objects.requireNonNull(arbeidstidsprosent, "arbeidstidsprosent");
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public BigDecimal getArbeidsprosent() {
        return arbeidsprosent;
    }
}
