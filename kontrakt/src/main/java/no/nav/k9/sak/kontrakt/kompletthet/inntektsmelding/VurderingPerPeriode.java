package no.nav.k9.sak.kontrakt.kompletthet.inntektsmelding;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class VurderingPerPeriode {

    @Size
    @NotNull
    @Valid
    @JsonProperty("vurderinger")
    private List<VurderingPåPeriode> vurderinger;

    public VurderingPerPeriode() {
    }

    public VurderingPerPeriode(@Size
                               @NotNull
                               @Valid
                               @JsonProperty("vurderinger") List<VurderingPåPeriode> vurderinger) {
        this.vurderinger = vurderinger;
    }

    public List<VurderingPåPeriode> getVurderinger() {
        return vurderinger;
    }
}
