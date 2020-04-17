package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UttaksperiodeOmsorgspenger {

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value = "utbetalingsgrad", required = true)
    @NotNull
    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal utbetalingsgrad;

    @JsonProperty(value="delvisFravær", required=false)
    @Valid
    private Duration delvisFravær;

    @JsonProperty(value="utfall", required=false)
    @Valid
    private OmsorgspengerUtfall utfall;

    @JsonCreator
    public UttaksperiodeOmsorgspenger(@JsonProperty(value = "periode", required = true) @Valid @NotNull Periode periode,
                                      @JsonProperty(value = "utbetalingsgrad", required = false) @Valid BigDecimal utbetalingsgrad,
                                      @JsonProperty(value = "utfall", required = false) @Valid OmsorgspengerUtfall utfall,
                                      @JsonProperty(value = "delvisFravær", required = false) @Valid Duration delvisFravær,
                                      @JsonProperty(value = "arbeidsforhold", required = false) @Valid UttakArbeidsforhold uttakArbeidsforhold) {
        this.periode = periode;
        this.utbetalingsgrad = utbetalingsgrad;
        this.utfall = utfall;
        this.delvisFravær = delvisFravær;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public void setUtbetalingsgrad(BigDecimal utbetalingsgrad) {
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public LocalDate getFom() {
        return getPeriode().getFom();
    }

    public LocalDate getTom() {
        return getPeriode().getTom();
    }

    public OmsorgspengerUtfall getUtfall() {
        return utfall;
    }

    public void setUtfall(OmsorgspengerUtfall utfall) {
        this.utfall = utfall;
    }



    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;

        var other = (UttaksperiodeOmsorgspenger) obj;
        return Objects.equals(periode, other.periode)
            && Objects.equals(utfall, other.utfall)
            && Objects.equals(utbetalingsgrad, other.utbetalingsgrad);
    }

    @Override
    public int hashCode() {
        return Objects.hash(periode, utfall, utbetalingsgrad);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "<periode=" + periode + ", utbetalingsgrad=" + utbetalingsgrad + ", utfall=" + utfall + ">";
    }

}
