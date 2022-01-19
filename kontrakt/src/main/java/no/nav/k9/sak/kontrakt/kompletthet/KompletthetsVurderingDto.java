package no.nav.k9.sak.kontrakt.kompletthet;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KompletthetsVurderingDto {

    @Size
    @NotNull
    @Valid
    @JsonProperty("tilstand")
    private List<KompletthetsTilstandPåPeriodeDto> kompletthetsTilstand;

    public KompletthetsVurderingDto(@Size
                                    @NotNull
                                    @JsonProperty("tilstand") List<KompletthetsTilstandPåPeriodeDto> kompletthetsTilstand) {
        this.kompletthetsTilstand = kompletthetsTilstand;
    }

    public List<KompletthetsTilstandPåPeriodeDto> getKompletthetsTilstand() {
        return kompletthetsTilstand;
    }

    @Override
    public String toString() {
        return "KompletthetsVurderingDto{" +
            "kompletthetsTilstand=" + kompletthetsTilstand +
            '}';
    }
}
