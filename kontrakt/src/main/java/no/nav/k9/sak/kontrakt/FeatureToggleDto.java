package no.nav.k9.sak.kontrakt;

import java.util.Map;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
public class FeatureToggleDto {

    @JsonProperty(value = "featureToggles")
    @Size(max = 50)
    private Map<String, Boolean> featureToggles;

    protected FeatureToggleDto() {
        //
    }

    public FeatureToggleDto(Map<String, Boolean> featureToggles) {
        this.featureToggles = featureToggles;
    }

    public Map<String, Boolean> getFeatureToggles() {
        return featureToggles;
    }
}
