package no.nav.k9.sak.kontrakt.kompletthet;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class KompletthetsVurderingV2Dto {

    @Size
    @NotNull
    @Valid
    @JsonProperty("tilstand")
    private List<KompletthetsTilstandPåPeriodeV2Dto> kompletthetsTilstand;

    public KompletthetsVurderingV2Dto(@Size
                                    @NotNull
                                    @JsonProperty("tilstand") List<KompletthetsTilstandPåPeriodeV2Dto> kompletthetsTilstand) {
        this.kompletthetsTilstand = kompletthetsTilstand;
    }

    public List<KompletthetsTilstandPåPeriodeV2Dto> getKompletthetsTilstand() {
        return kompletthetsTilstand;
    }

    @Override
    public String toString() {
        return "KompletthetsVurderingDto{" +
            "kompletthetsTilstand=" + kompletthetsTilstand +
            '}';
    }
}
