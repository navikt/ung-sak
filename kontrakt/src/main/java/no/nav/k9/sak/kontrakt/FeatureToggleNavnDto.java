package no.nav.k9.sak.kontrakt;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FeatureToggleNavnDto {
    
    @JsonProperty(value="navn", required = true)
    @Pattern(regexp = "^[a-zA-ZæøåÆØÅ_\\-0-9.]*$") ///samme som "^[\\p{L}\\p{N}_\\.\\-/]+$" men i tillegg punktum
    @Size(min = 1, max = 100)
    @NotNull
    private String navn;

    public FeatureToggleNavnDto() {
        //trengs for jackson
    }

    public FeatureToggleNavnDto(String navn) {
        this.navn = navn;
    }

    public String getNavn() {
        return navn;
    }

}
