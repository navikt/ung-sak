package no.nav.k9.sak.kontrakt;

import java.util.Collection;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FeatureToggleNavnListeDto {

    @JsonProperty(value="toggles", required = true)
    @Valid
    @NotNull
    @Size(min = 1, max = 10)
    private Collection<FeatureToggleNavnDto> toggles;

    public FeatureToggleNavnListeDto() {
        //trengs for jackson
    }

    public FeatureToggleNavnListeDto(Collection<FeatureToggleNavnDto> toggles) {
        this.toggles = toggles;
    }

    public Collection<FeatureToggleNavnDto> getToggles() {
        return toggles;
    }
}
