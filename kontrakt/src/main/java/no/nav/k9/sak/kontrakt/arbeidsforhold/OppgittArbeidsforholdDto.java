package no.nav.k9.sak.kontrakt.arbeidsforhold;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.typer.Beløp;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class OppgittArbeidsforholdDto {


    public OppgittArbeidsforholdDto() {
        //jackson
    }
    @JsonProperty(value = "periode")
    @Valid
    private PeriodeDto periode;

    @JsonProperty(value = "inntekt", required = true)
    @Valid
    @NotNull
    private Beløp inntekt;


    public PeriodeDto getPeriode() {
        return periode;
    }

    public void setPeriode(PeriodeDto periode) {
        this.periode = periode;
    }

    public Beløp getInntekt() {
        return inntekt;
    }

    public void setInntekt(Beløp inntekt) {
        this.inntekt = inntekt;
    }
}
