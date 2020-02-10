package no.nav.k9.sak.kontrakt;

import java.util.Map;

import javax.validation.Valid;
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
public class FeatureToggleDto {

    @JsonProperty(value = "featureToggles")
    @Size(max = 50)
    @Valid
    private Map<@Size(max = 100) @NotNull @Pattern(regexp = "^[\\p{Alnum}\\p{Space}\\p{L}\\p{N}\\p{P}]+$", message = "'${validatedValue}' matcher ikke tillatt pattern '{regexp}'") String, @NotNull Boolean> featureToggles;

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
