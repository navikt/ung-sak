package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class UttaksperiodeOmsorgspenger {

    @JsonProperty(value="periode", required=true)
    @Valid
    @NotNull
    private Periode periode;

    @JsonProperty(value="utbetalingsgrad", required=true)
    @Valid
    @NotNull
    private UttakUtbetalingsgrad utbetalingsgrad;

    @JsonProperty(value="utfall", required=true)
    @Valid
    @NotNull
    private OmsorgspengerUtfall utfall;


    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

    public UttakUtbetalingsgrad getUtbetalingsgrad() {
        return utbetalingsgrad;
    }

    public void setUtbetalingsgrad(UttakUtbetalingsgrad utbetalingsgrad) {
        this.utbetalingsgrad = utbetalingsgrad;
    }

    public OmsorgspengerUtfall getUtfall() {
        return utfall;
    }

    public void setUtfall(OmsorgspengerUtfall utfall) {
        this.utfall = utfall;
    }
}
