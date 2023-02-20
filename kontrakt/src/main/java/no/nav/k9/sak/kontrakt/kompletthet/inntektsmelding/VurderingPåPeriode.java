package no.nav.k9.sak.kontrakt.kompletthet.inntektsmelding;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import no.nav.k9.sak.typer.Periode;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VurderingPåPeriode {

    @NotNull
    @Valid
    @JsonProperty("periode")
    private Periode periode;

    @NotNull
    @Size()
    @Valid
    @JsonProperty("vurderinger")
    private List<InntektsmeldingVurdering> vurderinger;

    @JsonCreator
    public VurderingPåPeriode(@JsonProperty("periode") Periode periode,
                              @Valid @NotNull @Size @JsonProperty("vurderinger") List<@NotNull InntektsmeldingVurdering> vurderinger) {
        this.periode = periode;
        this.vurderinger = vurderinger;
    }

    public Periode getPeriode() {
        return periode;
    }

    public List<InntektsmeldingVurdering> getStatus() {
        return vurderinger;
    }

}
