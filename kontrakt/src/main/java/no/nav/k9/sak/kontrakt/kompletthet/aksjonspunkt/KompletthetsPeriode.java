package no.nav.k9.sak.kontrakt.kompletthet.aksjonspunkt;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import no.nav.k9.sak.kontrakt.uttak.Periode;


@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KompletthetsPeriode {

    @Valid
    @NotNull
    @JsonProperty("periode")
    private Periode periode;

    @NotNull
    @JsonProperty("fortsett")
    private Boolean kanFortsette;

    @JsonCreator
    public KompletthetsPeriode(@Valid @NotNull @JsonProperty(value = "periode", required = true) Periode periode,
                               @NotNull @JsonProperty(value = "fortsett", required = true) Boolean kanFortsette) {
        this.periode = periode;
        this.kanFortsette = kanFortsette;
    }

    public Periode getPeriode() {
        return periode;
    }

    public Boolean getKanFortsette() {
        return kanFortsette;
    }
}
