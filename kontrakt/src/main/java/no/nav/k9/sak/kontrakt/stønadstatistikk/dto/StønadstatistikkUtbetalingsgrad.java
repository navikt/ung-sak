package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import java.math.BigDecimal;
import java.time.Duration;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StønadstatistikkUtbetalingsgrad {
    
    @JsonProperty(value = "arbeidsforhold", required = true)
    @NotNull
    @Valid
    private StønadstatistikkArbeidsforhold arbeidsforhold;
    
    @JsonProperty(value = "normalArbeidstid", required = true)
    @NotNull
    @Valid
    private Duration normalArbeidstid;
    
    @JsonProperty(value = "faktiskArbeidstid", required = true)
    @NotNull
    @Valid
    private Duration faktiskArbeidstid;
    
    @JsonProperty(value = "utbetalingsgrad", required = true)
    @NotNull
    @Valid
    private BigDecimal utbetalingsgrad;
    
    @JsonProperty(value = "dagsats", required = true)
    @NotNull
    @Valid
    private int dagsats;

    
    protected StønadstatistikkUtbetalingsgrad() {
        
    }
    
    public StønadstatistikkUtbetalingsgrad(StønadstatistikkArbeidsforhold arbeidsforhold,
            Duration normalArbeidstid,
            Duration faktiskArbeidstid,
            BigDecimal utbetalingsgrad,
            int dagsats) {
        this.arbeidsforhold = arbeidsforhold;
        this.normalArbeidstid = normalArbeidstid;
        this.faktiskArbeidstid = faktiskArbeidstid;
        this.utbetalingsgrad = utbetalingsgrad;
        this.dagsats = dagsats;
    }
    

    public StønadstatistikkArbeidsforhold getArbeidsforhold() {
        return arbeidsforhold;
    }

    public Duration getNormalArbeidstid() {
        return normalArbeidstid;
    }

    public Duration getFaktiskArbeidstid() {
        return faktiskArbeidstid;
    }

    public BigDecimal getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public int getDagsats() {
        return dagsats;
    }
}
