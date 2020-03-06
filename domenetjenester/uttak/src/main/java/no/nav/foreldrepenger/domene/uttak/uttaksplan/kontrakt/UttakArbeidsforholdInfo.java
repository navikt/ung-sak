package no.nav.foreldrepenger.domene.uttak.uttaksplan.kontrakt;

import java.math.BigDecimal;
import java.time.Duration;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)

public class UttakArbeidsforholdInfo {

    @JsonProperty(value="jobberNormalt", required=true)
    @NotNull
    @Valid
    private Duration jobberNormalt;
    
    @JsonProperty(value="skalJobbe", required=true)
    @NotNull
    @DecimalMin(value = "0.00")
    @DecimalMax(value="100.00")
    private BigDecimal skalJobbe;

    public Duration getJobberNormalt() {
        return jobberNormalt;
    }

    public void setJobberNormalt(Duration jobberNormalt) {
        this.jobberNormalt = jobberNormalt;
    }

    public BigDecimal getSkalJobbe() {
        return skalJobbe;
    }

    public void setSkalJobbe(BigDecimal skalJobbe) {
        this.skalJobbe = skalJobbe;
    }
}
