package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.Duration;

public class FraværPeriodeOmsorgspenger {

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value="utbetalingsgrad", required = false)
    @Valid
    private UttakUtbetalingsgradOmsorgspenger utbetalingsgrad;


    @JsonProperty(value="delvisFravær", required=false)
    @Valid
    private Duration delvisFravær;

    @JsonProperty(value="arbeidsforhold", required = false)
    @Valid
    @NotNull
    private UttakArbeidsforhold uttakArbeidsforhold;

    @JsonProperty(value="utfall", required=false)
    @Valid
    private OmsorgspengerUtfall utfall;

    @JsonProperty(value="kreverRefusjon", required=false)
    @Valid
    private Boolean kreverRefusjon;

    @JsonCreator
    public FraværPeriodeOmsorgspenger(@JsonProperty(value = "periode", required = true) @Valid @NotNull Periode periode,
                                      @JsonProperty(value = "utbetalingsgrad", required = false) @Valid UttakUtbetalingsgradOmsorgspenger utbetalingsgrad,
                                      @JsonProperty(value = "utfall", required = false) @Valid OmsorgspengerUtfall utfall,
                                      @JsonProperty(value = "delvisFravær", required = false) @Valid Duration delvisFravær,
                                      @JsonProperty(value = "arbeidsforhold", required = false) @Valid UttakArbeidsforhold uttakArbeidsforhold) {
        this.periode = periode;
        this.utbetalingsgrad = utbetalingsgrad;
        this.utfall = utfall;
        this.delvisFravær = delvisFravær;
        this.uttakArbeidsforhold = uttakArbeidsforhold;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public UttakUtbetalingsgradOmsorgspenger getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public void setUtbetalingsgrad(UttakUtbetalingsgradOmsorgspenger utbetalingsgrad) {
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public Duration getDelvisFravær() {
        return delvisFravær;
    }

    public void setDelvisFravær(Duration delvisFravær) {
        this.delvisFravær = delvisFravær;
    }

    public UttakArbeidsforhold getUttakArbeidsforhold() {
        return uttakArbeidsforhold;
    }

    public void setUttakArbeidsforhold(UttakArbeidsforhold uttakArbeidsforhold) {
        this.uttakArbeidsforhold = uttakArbeidsforhold;
    }

    public OmsorgspengerUtfall getUtfall() {
        return utfall;
    }

    public void setUtfall(OmsorgspengerUtfall utfall) {
        this.utfall = utfall;
    }

    public Boolean getKreverRefusjon() {
        return kreverRefusjon;
    }

    public void setKreverRefusjon(Boolean kreverRefusjon) {
        this.kreverRefusjon = kreverRefusjon;
    }
}
