package no.nav.k9.sak.ytelse.omsorgspenger.utvidetrett.klient.modell;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE)
public class BehovResponse {

    @JsonProperty(value = "status", required = true)
    @NotNull
    @Valid
    private StatusType status;

    @JsonProperty(value = "potensielleStatuser")
    @Valid
    private Map<StatusType, RawJson> potensielleStatuser = new EnumMap<>(StatusType.class);

    @JsonProperty(value = "uløsteBehov")
    @Valid
    private Map<BehovType, RawJson> uløsteBehov = new EnumMap<>(BehovType.class);

    public BehovResponse() {
    }

    public StatusType getStatus() {
        return status;
    }

    public BehovResponse setStatus(StatusType status) {
        this.status = status;
        return this;
    }

    public Map<StatusType, RawJson> getPotensielleStatuser() {
        return Collections.unmodifiableMap(potensielleStatuser);
    }

    public Map<BehovType, RawJson> getUløsteBehov() {
        return Collections.unmodifiableMap(uløsteBehov);
    }

    public BehovResponse setPotensielleStatuser(Map<StatusType, RawJson> potensielleStatuser) {
        this.potensielleStatuser = Objects.requireNonNull(potensielleStatuser);
        return this;
    }

    public BehovResponse setUløsteBehov(Map<BehovType, RawJson> uløsteBehov) {
        this.uløsteBehov = Objects.requireNonNull(uløsteBehov);
        return this;
    }

}
