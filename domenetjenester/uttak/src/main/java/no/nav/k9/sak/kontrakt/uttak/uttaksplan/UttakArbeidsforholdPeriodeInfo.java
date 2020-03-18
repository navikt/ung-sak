package no.nav.k9.sak.kontrakt.uttak.uttaksplan;

import java.math.BigDecimal;
import java.time.Duration;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)

public class UttakArbeidsforholdPeriodeInfo {

    @JsonProperty(value = "jobberNormaltPerUke", required = true)
    @NotNull
    @Valid
    private Duration jobberNormaltPerUke;

    @JsonProperty(value = "skalJobbeProsent", required = true)
    @NotNull
    @DecimalMin(value = "0.00")
    @DecimalMax(value = "100.00")
    private BigDecimal skalJobbeProsent;

    @JsonCreator
    public UttakArbeidsforholdPeriodeInfo(@JsonProperty(value = "jobberNormaltPerUke", required = true) @NotNull @Valid Duration jobberNormaltPerUke,
                                          @JsonProperty(value = "skalJobbeProsent", required = true) @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal skalJobbeProsent) {
        this.jobberNormaltPerUke = jobberNormaltPerUke;
        this.skalJobbeProsent = skalJobbeProsent;
    }

    public Duration getJobberNormaltPerUke() {
        return jobberNormaltPerUke;
    }

    public BigDecimal getSkalJobbeProsent() {
        return skalJobbeProsent;
    }
}
