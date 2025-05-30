package no.nav.ung.sak.kontrakt.arbeidsforhold;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.ung.sak.typer.Beløp;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OppgittFrilansoppdragDto {

    public OppgittFrilansoppdragDto() {
        //jackson
    }

    @JsonProperty(value = "periode", required = true)
    @Valid
    @NotNull
    private PeriodeDto periode;

    @JsonProperty(value = "bruttoInntekt", required = true)
    @Valid
    @NotNull
    private Beløp bruttoInntekt;

    public PeriodeDto getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }

    public Beløp getBruttoInntekt() {
        return bruttoInntekt;
    }

    public void setBruttoInntekt(Beløp bruttoInntekt) {
        this.bruttoInntekt = bruttoInntekt;
    }
}
