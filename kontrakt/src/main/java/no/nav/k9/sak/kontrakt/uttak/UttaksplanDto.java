package no.nav.k9.sak.kontrakt.uttak;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Shape;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY, isGetterVisibility = Visibility.NONE)
public class UttaksplanDto {

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "behandlingUttaksplanerJson")
    @JsonRawValue
    private String behandlingUttaksplanerJson;

    @JsonInclude(value = Include.NON_EMPTY)
    @JsonProperty(value = "sakerUttaksplanerJson")
    @JsonRawValue
    private String sakerUttaksplanerJson;

    public UttaksplanDto(String behandlingUttaksplanerJson, String sakerUttaksplanerJson) {
        this.behandlingUttaksplanerJson = behandlingUttaksplanerJson;
        this.sakerUttaksplanerJson = sakerUttaksplanerJson;
    }

    public String getBehandlingUttaksplanerAsRawJson() {
        return behandlingUttaksplanerJson;
    }

    public String getSakerUttaksplanerAsRawJson() {
        return sakerUttaksplanerJson;
    }
}
