package no.nav.k9.sak.hendelse.stønadstatistikk.dto;

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

    
    public StønadstatistikkUtbetalingsgrad() {
        
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
    
    
}
