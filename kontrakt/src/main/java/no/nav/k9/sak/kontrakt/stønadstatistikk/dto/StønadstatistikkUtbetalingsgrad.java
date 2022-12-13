package no.nav.k9.sak.kontrakt.stønadstatistikk.dto;

import java.math.BigDecimal;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class StønadstatistikkUtbetalingsgrad {

    @JsonProperty(value = "aktivitetStatus", required = true)
    @Valid
    private String aktivitetStatus;

    @JsonProperty(value = "arbeidsforhold", required = true)
    @NotNull
    @Valid
    private StønadstatistikkArbeidsforhold arbeidsforhold;

    @JsonProperty(value = "normalArbeidstid", required = true)
    @Valid
    private Duration normalArbeidstid;

    @JsonProperty(value = "faktiskArbeidstid", required = true)
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

    @JsonProperty(value = "brukerErMottaker", required = true)
    @NotNull
    @Valid
    private boolean brukerErMottaker;


    public static StønadstatistikkUtbetalingsgrad forOmsorgspenger(String aktivitetStatus,
                                                                   StønadstatistikkArbeidsforhold arbeidsforhold,
                                                                   BigDecimal utbetalingsgrad,
                                                                   int dagsats,
                                                                   boolean brukerErMottaker) {
        StønadstatistikkUtbetalingsgrad su = new StønadstatistikkUtbetalingsgrad();
        su.aktivitetStatus = aktivitetStatus;
        su.arbeidsforhold = arbeidsforhold;
        su.utbetalingsgrad = utbetalingsgrad;
        su.dagsats = dagsats;
        su.brukerErMottaker = brukerErMottaker;
        return su;
    }

    protected StønadstatistikkUtbetalingsgrad() {
    }

    public StønadstatistikkUtbetalingsgrad(String aktivitetStatus,
                                           StønadstatistikkArbeidsforhold arbeidsforhold,
                                           Duration normalArbeidstid,
                                           Duration faktiskArbeidstid,
                                           BigDecimal utbetalingsgrad,
                                           int dagsats,
                                           boolean brukerErMottaker) {
        this.aktivitetStatus = aktivitetStatus;
        this.arbeidsforhold = arbeidsforhold;
        this.normalArbeidstid = normalArbeidstid;
        this.faktiskArbeidstid = faktiskArbeidstid;
        this.utbetalingsgrad = utbetalingsgrad;
        this.dagsats = dagsats;
        this.brukerErMottaker = brukerErMottaker;
    }

    public String getAktivitetStatus() {
        return aktivitetStatus;
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

    public boolean isBrukerErMottaker() {
        return brukerErMottaker;
    }
}
