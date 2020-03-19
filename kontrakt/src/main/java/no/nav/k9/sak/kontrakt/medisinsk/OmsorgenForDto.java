package no.nav.k9.sak.kontrakt.medisinsk;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class OmsorgenForDto {

    @JsonProperty(value = "morEllerFar")
    private Boolean morEllerFar;

    @JsonProperty(value = "sammeBosted")
    private Boolean sammeBosted;

    @JsonCreator
    public OmsorgenForDto(@JsonProperty(value = "sammeBosted") Boolean morEllerFar, @JsonProperty(value = "sammeBosted") Boolean sammeBosted) {
        this.morEllerFar = morEllerFar;
        this.sammeBosted = sammeBosted;
    }

    public Boolean getMorEllerFar() {
        return morEllerFar;
    }

    public Boolean getSammeBosted() {
        return sammeBosted;
    }
}
